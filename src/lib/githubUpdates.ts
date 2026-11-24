import type { GitHubReleaseNotice } from '../types/ledger';

export const APP_VERSION_NAME = '1.0.32';
export const APP_VERSION_CODE = 39;

export async function checkForLatestGitHubRelease(
  repoSlug: string
): Promise<GitHubReleaseNotice | null> {
  const clean = repoSlug.trim();
  if (!clean) return null;

  const params = new URLSearchParams({
    repo: clean,
    currentVersion: APP_VERSION_NAME,
    currentCode: String(APP_VERSION_CODE),
  });

  const resp = await fetch(`/api/github-release?${params.toString()}`);
  if (!resp.ok) {
    throw new Error('Failed to check GitHub releases.');
  }
  const data = (await resp.json()) as { notice?: GitHubReleaseNotice | null };
  return data.notice ?? null;
}
