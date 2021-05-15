package hk.karasshop.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import hk.karasshop.R
import hk.karasshop.models.User
import hk.karasshop.utils.Constants
import kotlinx.android.synthetic.main.activity_user_profile.*

class UserProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        var userDetails: User = User()
        if (intent.hasExtra(Constants.EXTRA_USER_DETAILS)) {
            userDetails = intent.getParcelableExtra(Constants.EXTRA_USER_DETAILS)!!
        }
        et_first_name.isEnabled = false
        et_first_name.setText(userDetails.firstName)
        et_last_name.isEnabled = false
        et_last_name.setText(userDetails.lastName)
        et_email.isEnabled = false
        et_email.setText(userDetails.email)
    }
}