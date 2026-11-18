package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GitHubReleaseNotice
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class GitHubReleaseAssetDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "browser_download_url") val browserDownloadUrl: String? = null
)

data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "draft") val draft: Boolean = false,
    @Json(name = "prerelease") val prerelease: Boolean = false,
    @Json(name = "assets") val assets: List<GitHubReleaseAssetDto> = emptyList()
)

data class RepoApkManifestDto(
    @Json(name = "versionName") val versionName: String? = null,
    @Json(name = "versionCode") val versionCode: Int = 0,
    @Json(name = "apkFile") val apkFile: String? = null,
    @Json(name = "notes") val notes: String? = null
)

interface GitHubApiService {
    @Headers("Accept: application/vnd.github+json", "User-Agent: SageLedger-Android")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto

    @Headers("User-Agent: SageLedger-Android")
    @GET
    suspend fun getApkManifest(@Url url: String): RepoApkManifestDto
}

class GitHubUpdateRepository {

    private val api: GitHubApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }

    suspend fun checkForUpdate(
        repoSlug: String,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): Result<GitHubReleaseNotice?> = withContext(Dispatchers.IO) {
        val clean = repoSlug.trim().removePrefix("https://github.com/").trim('/')
        val parts = clean.split('/').filter { it.isNotBlank() }
        if (parts.size != 2) {
            return@withContext Result.success(null)
        }
        val owner = parts[0]
        val repo = parts[1]

        try {
            val releaseNotice = checkLatestRelease(owner, repo, currentVersionName)
            if (releaseNotice != null) {
                return@withContext Result.success(releaseNotice)
            }

            val folderNotice = checkApkFolderManifest(owner, repo, currentVersionName, currentVersionCode)
            Result.success(folderNotice)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "GitHub update check failed for $clean: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun checkLatestRelease(
        owner: String,
        repo: String,
        currentVersionName: String
    ): GitHubReleaseNotice? {
        val dto = try {
            api.getLatestRelease(owner, repo)
        } catch (e: HttpException) {
            if (e.code() == 404) return null else throw e
        }
        if (dto.draft || dto.prerelease) return null
        val tag = dto.tagName?.trim().orEmpty()
        val apkAssetUrl = dto.assets
            .firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
            ?.browserDownloadUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val targetUrl = apkAssetUrl ?: dto.htmlUrl?.trim().orEmpty()
        if (tag.isBlank() || targetUrl.isBlank()) return null

        return if (isNewerVersion(remoteTag = tag, localVersion = currentVersionName)) {
            GitHubReleaseNotice(
                tagName = tag,
                releaseName = dto.name?.takeIf { it.isNotBlank() } ?: "Update $tag",
                notes = dto.body?.takeIf { it.isNotBlank() }
                    ?: "A new release ($tag) is available on GitHub.",
                htmlUrl = targetUrl
            )
        } else {
            null
        }
    }

    private suspend fun checkApkFolderManifest(
        owner: String,
        repo: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): GitHubReleaseNotice? {
        val branches = listOf("main", "master")
        for (branch in branches) {
            val manifestUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/apk/version.json"
            val manifest = try {
                api.getApkManifest(manifestUrl)
            } catch (e: HttpException) {
                if (e.code() == 404) continue else throw e
            }
            val remoteName = manifest.versionName?.trim().orEmpty()
            val apkFile = manifest.apkFile?.trim()?.takeIf { it.isNotBlank() } ?: "SageLedger-latest.apk"
            val isNewer = (manifest.versionCode > currentVersionCode) ||
                (remoteName.isNotBlank() && isNewerVersion(remoteName, currentVersionName))
            if (isNewer) {
                val tag = if (remoteName.startsWith("v", ignoreCase = true)) remoteName else "v$remoteName"
                val downloadUrl = "https://github.com/$owner/$repo/raw/$branch/apk/$apkFile"
                return GitHubReleaseNotice(
                    tagName = tag,
                    releaseName = "New APK build ($tag)",
                    notes = manifest.notes?.takeIf { it.isNotBlank() }
                        ?: "A new APK build is ready in the repository apk/ folder.",
                    htmlUrl = downloadUrl
                )
            }
            return null
        }
        return null
    }

    companion object {
        private const val TAG = "GitHubUpdateRepo"

        fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
            val remoteParts = parseSemver(remoteTag)
            val localParts = parseSemver(localVersion)
            if (remoteParts.isEmpty() || localParts.isEmpty()) {
                return remoteTag.removePrefix("v") != localVersion.removePrefix("v")
            }
            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }

        private fun parseSemver(version: String): List<Int> =
            version.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .split('.')
                .mapNotNull { it.toIntOrNull() }
    }
}
