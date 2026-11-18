package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GitHubReleaseNotice
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "draft") val draft: Boolean = false,
    @Json(name = "prerelease") val prerelease: Boolean = false
)

interface GitHubApiService {
    @Headers("Accept: application/vnd.github+json", "User-Agent: SageLedger-Android")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto
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
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): Result<GitHubReleaseNotice?> = withContext(Dispatchers.IO) {
        val clean = repoSlug.trim().removePrefix("https://github.com/").trim('/')
        val parts = clean.split('/').filter { it.isNotBlank() }
        if (parts.size != 2) {
            return@withContext Result.success(null)
        }

        try {
            val dto = api.getLatestRelease(parts[0], parts[1])
            if (dto.draft || dto.prerelease) {
                return@withContext Result.success(null)
            }
            val tag = dto.tagName?.trim().orEmpty()
            val htmlUrl = dto.htmlUrl?.trim().orEmpty()
            if (tag.isBlank() || htmlUrl.isBlank()) {
                return@withContext Result.success(null)
            }
            if (isNewerVersion(remoteTag = tag, localVersion = currentVersionName)) {
                Result.success(
                    GitHubReleaseNotice(
                        tagName = tag,
                        releaseName = dto.name?.takeIf { it.isNotBlank() } ?: "Update $tag",
                        notes = dto.body?.takeIf { it.isNotBlank() }
                            ?: "A new release ($tag) is available on GitHub.",
                        htmlUrl = htmlUrl
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "GitHub release check skipped/failed for $clean: ${e.message}")
            Result.failure(e)
        }
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
