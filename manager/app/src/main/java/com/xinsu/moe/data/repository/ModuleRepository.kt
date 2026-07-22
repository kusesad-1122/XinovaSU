package com.xinsu.moe.data.repository

import com.xinsu.moe.data.model.Module
import com.xinsu.moe.data.model.ModuleUpdateInfo

interface ModuleRepository {
    suspend fun getModules(): Result<List<Module>>
    suspend fun checkUpdate(module: Module): Result<ModuleUpdateInfo>
}
