package com.geekymusketeers.medify.ui.auth.signInScreen

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.geekymusketeers.medify.base.BaseViewModel
import com.geekymusketeers.medify.model.User
import com.geekymusketeers.medify.utils.Constants
import com.geekymusketeers.medify.utils.Logger
import com.geekymusketeers.medify.utils.SharedPrefsExtension.saveUserToSharedPrefs
import com.geekymusketeers.medify.utils.Validator.Companion.isValidEmail
import com.geekymusketeers.medify.utils.Validator.Companion.isValidPassword
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel(application: Application) : BaseViewModel(application) {

    // LiveData fields to hold email, password, user data, etc.
    private val email = MutableLiveData<String>()
    private val password = MutableLiveData<String>()
    var enableLoginButton = MutableLiveData<Boolean>()
    var userIDLiveData = MutableLiveData<String>()
    var userLiveData = MutableLiveData<User>()
    var errorLiveData = MutableLiveData<String>()
    var sharedPreferenceLiveData = MutableLiveData<Boolean>()

    // Repository for handling sign-in logic
    private val signInRepository: SignInRepository = SignInRepository()

    // Setters for email and password fields
    fun setEmail(email: String) {
        this.email.value = email
        updateButtonState()  // Update the login button state when email is set
    }

    fun setPassword(password: String) {
        this.password.value = password
        updateButtonState()  // Update the login button state when password is set
    }

    // Function to log in the user
    fun login() = viewModelScope.launch(Dispatchers.IO) {
        val emailInput = email.value
        val passwordInput = password.value

        // Validate email input
        if (emailInput.isNullOrEmpty() || emailInput.isValidEmail().not()) {
            errorLiveData.postValue("Please enter a valid email")
            return@launch
        }

        // Validate password input
        if (passwordInput.isNullOrEmpty() || passwordInput.isValidPassword().not()) {
            errorLiveData.postValue("Please enter a valid password")
            return@launch
        }

        // Attempt to log in using the repository
        val user = signInRepository.loginUser(emailInput, passwordInput)

        if (user == null) {
            errorLiveData.postValue("Please check your details")
            return@launch
        } else {
            // If login is successful, post the user ID to LiveData
            userIDLiveData.postValue(user.uid)
        }
    }

    // Function to get user data from Firebase
    fun getUserFromFirebase() = viewModelScope.launch(Dispatchers.IO) {
        val userId = userIDLiveData.value

        // Check if userId is null before making the Firebase query
        if (userId != null) {
            FirebaseDatabase.getInstance().reference.child(Constants.Users).child(userId)
                .get().addOnSuccessListener { dataSnapshot ->
                    try {
                        // Map Firebase data to User object
                        val user = User(
                            UID = dataSnapshot.child("uid").value.toString().trim(),
                            Name = dataSnapshot.child("name").value.toString().trim(),
                            Age = dataSnapshot.child("age").value.toString().trim().toInt(),
                            Email = dataSnapshot.child("email").value.toString().trim(),
                            Phone = dataSnapshot.child("phone").value.toString().trim(),
                            isDoctor = dataSnapshot.child("doctor").value.toString().trim(),
                            Specialist = dataSnapshot.child("specialist").value.toString().trim(),
                            Gender = dataSnapshot.child("gender").value.toString().trim(),
                            Address = dataSnapshot.child("address").value.toString().trim(),
                            Stats = dataSnapshot.child("stats").value.toString().trim(),
                            Prescription = dataSnapshot.child("prescription").value.toString().trim(),
                        )

                        // Post user data to LiveData
                        userLiveData.postValue(user)
                    } catch (e: Exception) {
                        Logger.debugLog("Error: ${e.message} and age is ${dataSnapshot.child("age").value.toString().trim()}")
                        errorLiveData.postValue(e.message.toString())
                    }
                }.addOnFailureListener {
                    // Handle any failures in the Firebase query
                    errorLiveData.postValue(it.message.toString())
                }
        } else {
            // Handle the case where userID is null
            errorLiveData.postValue("User ID is null.")
        }
    }

    // Function to save user data to SharedPreferences
    fun saveInSharedPreference(sharedPreferences: SharedPreferences) {
        val user = userLiveData.value

        // Check if user is not null before saving to SharedPreferences
        if (user != null) {
            sharedPreferences.saveUserToSharedPrefs(user)
            sharedPreferenceLiveData.postValue(true)
        } else {
            // Post error message if user is null
            errorLiveData.postValue("User data is missing. Cannot save to shared preferences.")
        }
    }

    // Function to update the login button state
    private fun updateButtonState() {
        val requiredField = email.value.isNullOrEmpty() || password.value.isNullOrEmpty()
        enableLoginButton.value = requiredField.not()
    }
}
