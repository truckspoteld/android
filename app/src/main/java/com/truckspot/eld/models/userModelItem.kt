import com.google.gson.annotations.SerializedName

data class userModelItem(
 val id: Int,
 val username: String,
 val email: String,
 val role: String,
 val companyId: Int,
 val isAdded: String,
 val isActive: String,
 val mobile: String,
 val name: String,
 val firstName: String,
 val lastName: String,
 val licenseNumber: String,
 val licenseDate: String?,
 val licenseState: String,
 val orderNumber: String,
 val coDriver: String
)
