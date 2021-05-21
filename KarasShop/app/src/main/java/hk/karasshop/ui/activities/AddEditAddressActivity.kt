package hk.karasshop.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import hk.karasshop.R
import hk.karasshop.firestore.FirestoreClass
import hk.karasshop.models.Address
import hk.karasshop.utils.Constants
import kotlinx.android.synthetic.main.activity_add_edit_address.*
import java.util.*

class AddEditAddressActivity : BaseActivity() {

    private var mAddressDetails: Address? = null
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_address)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (intent.hasExtra(Constants.EXTRA_ADDRESS_DETAILS)) {
            mAddressDetails =
                intent.getParcelableExtra(Constants.EXTRA_ADDRESS_DETAILS)!!
        }
        setupActionBar()
        if (mAddressDetails != null) {
            if (mAddressDetails!!.id.isNotEmpty()) {
                tv_title.text = resources.getString(R.string.title_edit_address)
                btn_submit_address.text = resources.getString(R.string.btn_lbl_update)
                et_full_name.setText(mAddressDetails?.name)
                et_phone_number.setText(mAddressDetails?.mobileNumber)
                et_address.setText(mAddressDetails?.address)
                et_zip_code.setText(mAddressDetails?.zipCode)
                et_additional_note.setText(mAddressDetails?.additionalNote)
                when (mAddressDetails?.type) {
                    Constants.HOME -> {
                        rb_home.isChecked = true
                    }
                    Constants.OFFICE -> {
                        rb_office.isChecked = true
                    }
                    else -> {
                        rb_other.isChecked = true
                        til_other_details.visibility = View.VISIBLE
                        et_other_details.setText(mAddressDetails?.otherDetails)
                    }
                }
            }
        }
        rg_type.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_other) {
                til_other_details.visibility = View.VISIBLE
            } else {
                til_other_details.visibility = View.GONE
            }
        }
        btn_submit_address.setOnClickListener {
            saveAddressToFirestore()
        }
        btn_google_map.setOnClickListener {
            //val intent = Intent(this@AddEditAddressActivity, MapsActivity::class.java)
            //startActivity(intent)
            getLastLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.INTERNET_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Debug", "PERMISSION_GRANTED")
                //Constants.showImageChooser(this@AddEditAddressActivity)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.google_map_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_add_edit_address_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }
        toolbar_add_edit_address_activity.setNavigationOnClickListener { onBackPressed() }
    }

    private fun validateData(): Boolean {
        return when {
            TextUtils.isEmpty(et_full_name.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar(
                    resources.getString(R.string.err_msg_please_enter_full_name),
                    true
                )
                false
            }
            TextUtils.isEmpty(et_phone_number.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar(
                    resources.getString(R.string.err_msg_please_enter_phone_number),
                    true
                )
                false
            }
            TextUtils.isEmpty(et_address.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_please_enter_address), true)
                false
            }
            TextUtils.isEmpty(et_zip_code.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_please_enter_zip_code), true)
                false
            }
            rb_other.isChecked && TextUtils.isEmpty(
                et_zip_code.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_please_enter_zip_code), true)
                false
            }
            else -> {
                true
            }
        }
    }

    private fun saveAddressToFirestore() {
        val fullName: String = et_full_name.text.toString().trim { it <= ' ' }
        val phoneNumber: String = et_phone_number.text.toString().trim { it <= ' ' }
        val address: String = et_address.text.toString().trim { it <= ' ' }
        val zipCode: String = et_zip_code.text.toString().trim { it <= ' ' }
        val additionalNote: String = et_additional_note.text.toString().trim { it <= ' ' }
        val otherDetails: String = et_other_details.text.toString().trim { it <= ' ' }
        if (validateData()) {
            showProgressDialog(resources.getString(R.string.please_wait))
            val addressType: String = when {
                rb_home.isChecked -> {
                    Constants.HOME
                }
                rb_office.isChecked -> {
                    Constants.OFFICE
                }
                else -> {
                    Constants.OTHER
                }
            }
            val addressModel = Address(
                FirestoreClass().getCurrentUserID(),
                fullName,
                phoneNumber,
                address,
                zipCode,
                additionalNote,
                addressType,
                otherDetails
            )
            if (mAddressDetails != null && mAddressDetails!!.id.isNotEmpty()) {
                FirestoreClass().updateAddress(
                    this@AddEditAddressActivity,
                    addressModel,
                    mAddressDetails!!.id
                )
            } else {
                FirestoreClass().addAddress(this@AddEditAddressActivity, addressModel)
            }
        }
    }

    fun addUpdateAddressSuccess() {
        hideProgressDialog()
        val notifySuccessMessage: String =
            if (mAddressDetails != null && mAddressDetails!!.id.isNotEmpty()) {
                resources.getString(R.string.msg_your_address_updated_successfully)
            } else {
                resources.getString(R.string.err_your_address_added_successfully)
            }
        Toast.makeText(
            this@AddEditAddressActivity,
            notifySuccessMessage,
            Toast.LENGTH_SHORT
        ).show()
        setResult(RESULT_OK)
        finish()
    }

    fun CheckPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    fun RequestPermssion() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            Constants.INTERNET_CODE
        )
    }

    fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getLastLocation() {
        if (CheckPermission()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                        var location = task.result
                        if (location == null) {
                            getNewLocation()
                        } else {
                            et_additional_note.setText(
                                "Address:"+getCountryName(location.latitude, location.longitude)+" "+
                                        getCityName(location.latitude, location.longitude)+" "+
                                        areaName(location.latitude,location.longitude)
                            )
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.google_map_enabled),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            RequestPermssion();
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            var lastLocation = p0.lastLocation
            et_additional_note.setText(
                "Address:"+getCountryName(lastLocation.latitude, lastLocation.longitude)+" "+
                        getCityName(lastLocation.latitude, lastLocation.longitude)+" "+
                        areaName(lastLocation.latitude,lastLocation.longitude)
            )
       }
    }

    fun getNewLocation() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 2
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient!!.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
        }
    }

    fun getCityName(lat: Double, long: Double): String {
        var CityName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(lat, long, 1)
        CityName = address.get(0).locality
        return CityName
    }

    fun getCountryName(lat: Double, long: Double): String {
        var CountryName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(lat, long, 1)
        CountryName = address.get(0).countryName
        return CountryName
    }

    fun areaName(lat: Double, long: Double): String {
        var areaName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(lat, long, 1)
        areaName = address.get(0).thoroughfare
        return areaName
    }
}