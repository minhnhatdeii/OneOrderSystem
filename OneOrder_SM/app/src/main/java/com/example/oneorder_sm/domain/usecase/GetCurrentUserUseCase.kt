package com.example.oneorder_sm.domain.usecase

import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Profile?> {
        return authRepository.currentUser
    }
}
