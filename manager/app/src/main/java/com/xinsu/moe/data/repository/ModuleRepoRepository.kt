package com.xinsu.moe.data.repository

import com.xinsu.moe.data.model.RepoModuleDetail
import com.xinsu.moe.data.model.RepoModuleList
import com.xinsu.moe.data.registry.UpdateVerification

interface ModuleRepoRepository {
    /** Index + module list; falls back to the last complete cached copy on failure. */
    suspend fun fetchModules(): Result<RepoModuleList>

    /**
     * Detail (and readme) of a module in the current index. Fails with
     * [com.xinsu.moe.data.registry.ModuleNotInIndexException] if it is not listed.
     */
    suspend fun fetchDetail(moduleId: String): Result<RepoModuleDetail>

    /** Sanitized changelog HTML of a release (by tag, or the stable release), or null. */
    suspend fun fetchChangelogHtml(moduleId: String, tag: String?): String?

    /**
     * How an `updateJson` update of an installed module must be verified: against the matching
     * registry release, rejected, or legacy (not a registry update).
     */
    suspend fun resolveUpdate(moduleId: String, updateJsonUrl: String, zipUrl: String, versionCode: Int): UpdateVerification

    /** Raw Markdown of a registry-hosted document (e.g. `changelog.md`), or null if not registry-hosted. */
    suspend fun fetchRegistryText(url: String): String?

    /** Whether [url] is served by the registry site (its content must use the restricted renderer). */
    fun isRegistryUrl(url: String): Boolean
}
