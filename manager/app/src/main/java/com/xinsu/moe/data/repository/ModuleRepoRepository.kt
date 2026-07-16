package com.xinsu.moe.data.repository

import com.xinsu.moe.data.model.RepoModule

interface ModuleRepoRepository {
    suspend fun fetchModules(): Result<List<RepoModule>>
}
