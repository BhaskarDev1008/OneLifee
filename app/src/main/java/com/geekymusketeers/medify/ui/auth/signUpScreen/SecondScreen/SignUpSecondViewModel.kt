package com.geekymusketeers.medify.ui.auth.signUpScreen.SecondScreen

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.geekymusketeers.medify.base.BaseViewModel
import com.geekymusketeers.medify.model.Doctor
import com.geekymusketeers.medify.model.User
import com.geekymusketeers.medify.ui.auth.signUpScreen.SignUpRepository
import com.geekymusketeers.medify.utils.Constants
import com.geekymusketeers.medify.utils.Logger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpSecondViewModel(application: Application) : BaseViewModel(application) {

    // LiveData to observe in UI
    private var userPassword = MutableLiveData<String>()
    private var userLiveData = MutableLiveData<User>()
    private var userAddress = MutableLiveData<String>()
    var userIsDoctor = MutableLiveData<String>()
    private var userSpecialization = MutableLiveData<String>()
    var userAccountCreationLiveData = MutableLiveData<Boolean>()
    var userDataBaseUpdate = MutableLiveData<Boolean>()
    var errorLiveData = MutableLiveData<String>()
    private val signInRepository: SignUpRepository = SignUpRepository()

    val enableCreateAccountButtonLiveData: MutableLiveData<Boolean> by lazy { MutableLiveData() }

    // Initialize default value for doctor flag
    init {
        userIsDoctor.value = Doctor.IS_NOT_DOCTOR.toItemString()
    }

    // Set user password
    fun setUserPassword(password: String) {
        userPassword.value = password
    }

    // Set user information
    fun setUpUser(user: User) {
        userLiveData.value = user
    }

    // Function to create Firebase user account
    fun createAccount() = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Extracting user data
            val email = userLiveData.value?.Email ?: throw Exception("Email is null")
            val password = userPassword.value ?: throw Exception("Password is null")
            val address = userAddress.value
            val isDoctor = userIsDoctor.value
            val specialization = if (isDoctor == Doctor.IS_DOCTOR.toItemString()) userSpecialization.value else null

            // Updating user data locally
            userLiveData.value?.apply {
                Address = address
                if (isDoctor != null) this.isDoctor = isDoctor
                Specialist = specialization
            }

            // Registering user via repository (assuming proper function in SignUpRepository)
            val userID = signInRepository.registerUser(email, password)?.uid
            Logger.debugLog("User ID after account creation is $userID")

            if (userID == null) {
                errorLiveData.postValue("Please check your details (User is null)")
                return@launch
            }

            // Updating user UID and saving data in Firebase Realtime Database
            userLiveData.value?.UID = userID
            FirebaseDatabase.getInstance().reference.child(Constants.Users).child(userID)
                .setValue(userLiveData.value)
                .addOnSuccessListener {
                    FirebaseAuth.getInstance().signOut() // Sign out after account creation
                    Logger.debugLog("User database created successfully and userID is $userID")
                    userAccountCreationLiveData.postValue(true) // Notify account creation success
                }
                .addOnFailureListener {
                    Logger.debugLog("Exception caught at creating user database: ${it.message}")
                    errorLiveData.postValue(it.message) // Notify failure to create user database
                }

        } catch (e: Exception) {
            Logger.debugLog("Error during account creation: ${e.message}")
            errorLiveData.postValue(e.message) // Notify any other failure
        }
    }

    // Function to create user database if user already exists
    fun createUserDatabase() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val firebaseAuth = FirebaseAuth.getInstance()
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("User ID not found")
            val userReference = FirebaseDatabase.getInstance().reference.child(Constants.Users).child(userId)

            // Saving user data to Realtime Database
            userReference.setValue(userLiveData.value).addOnSuccessListener {
                Logger.debugLog("User database created successfully and userID is $userId")
                userDataBaseUpdate.postValue(true)
            }.addOnFailureListener {
                Logger.debugLog("Exception caught at creating user database: ${it.message}")
                userDataBaseUpdate.postValue(false)
            }

            // If the user is a doctor, create doctor database entry
            if (userLiveData.value?.isDoctor == Doctor.IS_DOCTOR.toItemString()) {
                FirebaseDatabase.getInstance().reference.child(Constants.Doctor).child(userId)
                    .setValue(userLiveData.value)
                    .addOnSuccessListener {
                        Logger.debugLog("Doctor database created successfully")
                    }.addOnFailureListener {
                        Logger.debugLog("Exception caught at creating doctor database: ${it.message}")
                    }
            }
        } catch (e: Exception) {
            Logger.debugLog("Error creating user database: ${e.message}")
            errorLiveData.postValue(e.message)
        }
    }

    // Set user address and validate form
    fun setUserAddress(address: String) {
        userAddress.value = address
        updateButtonState()
    }

    // Set if user is a doctor and validate form
    fun setUserIsDoctor(isDoctor: String) {
        userIsDoctor.value = isDoctor
        updateButtonState()
    }

    // Set user specialization and validate form
    fun setUserSpecialization(specialization: String) {
        userSpecialization.value = specialization
        updateButtonState()
    }

    // Validate if the "Create Account" button should be enabled based on form state
    private fun updateButtonState() {
        val requiredField =
            userAddress.value.isNullOrEmpty() || if (userIsDoctor.value == Doctor.IS_DOCTOR.toItemString()) {
                userSpecialization.value.isNullOrEmpty()
            } else {
                false
            }
        enableCreateAccountButtonLiveData.value = requiredField.not()
    }
}
