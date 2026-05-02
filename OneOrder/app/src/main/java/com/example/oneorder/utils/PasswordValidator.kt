package com.example.oneorder.utils

import android.util.Patterns

/**
 * PasswordValidator - Validates password strength according to security standards
 *
 * Validation Rules:
 * - Minimum 8 characters
 * - At least 1 uppercase letter (A-Z)
 * - At least 1 lowercase letter (a-z)
 * - At least 1 digit (0-9)
 * - At least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
 * - No whitespace characters
 * - No more than 3 consecutive identical characters
 */
object PasswordValidator {

    enum class PasswordStrength(val label: String, val color: String, val progress: Float) {
        WEAK("Yếu", "red", 0.33f),
        MEDIUM("Trung bình", "orange", 0.66f),
        STRONG("Mạnh", "green", 1.0f)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val strength: PasswordStrength,
        val errors: List<String> = emptyList()
    )

    data class PasswordRequirement(
        val label: String,
        val isMet: Boolean
    )

    private const val MIN_LENGTH = 8
    private const val MAX_CONSECUTIVE_CHARS = 3

    /**
     * Validate password and return detailed result
     */
    fun validate(password: String, email: String = ""): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check each requirement
        if (password.length < MIN_LENGTH) {
            errors.add("Mật khẩu phải có ít nhất $MIN_LENGTH ký tự")
        }
        
        if (!password.any { it.isUpperCase() }) {
            errors.add("Phải có ít nhất 1 chữ hoa (A-Z)")
        }
        
        if (!password.any { it.isLowerCase() }) {
            errors.add("Phải có ít nhất 1 chữ thường (a-z)")
        }
        
        if (!password.any { it.isDigit() }) {
            errors.add("Phải có ít nhất 1 số (0-9)")
        }
        
        if (!hasSpecialCharacter(password)) {
            errors.add("Phải có ít nhất 1 ký tự đặc biệt (!@#\$%^&*)")
        }
        
        if (password.any { it.isWhitespace() }) {
            errors.add("Không được chứa khoảng trắng")
        }
        
        if (hasConsecutiveChars(password, MAX_CONSECUTIVE_CHARS)) {
            errors.add("Không được có quá $MAX_CONSECUTIVE_CHARS ký tự liên tiếp giống nhau")
        }
        
        if (email.isNotBlank() && password.contains(email, ignoreCase = true)) {
            errors.add("Mật khẩu không được chứa email")
        }
        
        val strength = calculateStrength(password)
        val isValid = errors.isEmpty()
        
        return ValidationResult(isValid, strength, errors)
    }

    /**
     * Quick check if password meets minimum requirements
     */
    fun isPasswordValid(password: String): Boolean {
        return password.length >= MIN_LENGTH &&
               password.any { it.isUpperCase() } &&
               password.any { it.isLowerCase() } &&
               password.any { it.isDigit() } &&
               hasSpecialCharacter(password) &&
               !password.any { it.isWhitespace() } &&
               !hasConsecutiveChars(password, MAX_CONSECUTIVE_CHARS)
    }

    /**
     * Calculate password strength without full validation
     */
    fun calculateStrength(password: String): PasswordStrength {
        var score = 0
        
        // Length scoring
        when {
            password.length >= 16 -> score += 3
            password.length >= 12 -> score += 2
            password.length >= MIN_LENGTH -> score += 1
        }
        
        // Character type scoring
        if (password.any { it.isUpperCase() }) score += 1
        if (password.any { it.isLowerCase() }) score += 1
        if (password.any { it.isDigit() }) score += 1
        if (hasSpecialCharacter(password)) score += 1
        
        // Penalty for common patterns
        if (password.contains(Regex("(.)\\1{2,}"))) score -= 1
        if (isCommonPassword(password)) score -= 2
        
        return when {
            score >= 6 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    /**
     * Get list of password requirements with their status
     */
    fun getPasswordRequirements(password: String): List<PasswordRequirement> {
        return listOf(
            PasswordRequirement(
                label = "Ít nhất $MIN_LENGTH ký tự (${password.length}/$MIN_LENGTH)",
                isMet = password.length >= MIN_LENGTH
            ),
            PasswordRequirement(
                label = "Ít nhất 1 chữ hoa (A-Z)",
                isMet = password.any { it.isUpperCase() }
            ),
            PasswordRequirement(
                label = "Ít nhất 1 chữ thường (a-z)",
                isMet = password.any { it.isLowerCase() }
            ),
            PasswordRequirement(
                label = "Ít nhất 1 số (0-9)",
                isMet = password.any { it.isDigit() }
            ),
            PasswordRequirement(
                label = "Ít nhất 1 ký tự đặc biệt (!@#\$%^&*)",
                isMet = hasSpecialCharacter(password)
            ),
            PasswordRequirement(
                label = "Không có khoảng trắng",
                isMet = !password.any { it.isWhitespace() }
            )
        )
    }

    /**
     * Check if password contains special character
     */
    private fun hasSpecialCharacter(password: String): Boolean {
        val specialChars = setOf(
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+',
            '-', '=', '[', ']', '{', '}', '|', ';', ':', ',', '.', '<', '>', '?', '/', '`', '~'
        )
        return password.any { it in specialChars }
    }

    /**
     * Check for consecutive identical characters
     */
    private fun hasConsecutiveChars(password: String, maxConsecutive: Int): Boolean {
        if (password.length < maxConsecutive) return false
        
        var count = 1
        for (i in 1 until password.length) {
            if (password[i] == password[i - 1]) {
                count++
                if (count >= maxConsecutive) return true
            } else {
                count = 1
            }
        }
        return false
    }

    /**
     * Check for common weak passwords
     */
    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            "password", "password123", "123456", "12345678", "123456789",
            "qwerty", "abc123", "monkey", "master", "dragon",
            "letmein", "login", "admin", "welcome", "shadow",
            "sunshine", "princess", "football", "iloveyou", "trustno1"
        )
        return password.lowercase() in commonPasswords
    }

    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Get email validation error message
     */
    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email không được để trống"
            !isValidEmail(email) -> "Định dạng email không hợp lệ"
            else -> null
        }
    }
}
