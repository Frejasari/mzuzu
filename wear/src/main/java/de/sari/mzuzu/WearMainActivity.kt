package de.sari.mzuzu

import android.os.Bundle
import android.support.wearable.activity.WearableActivity

class WearMainActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wear_activity_main)

        // Enables Always-on
        setAmbientEnabled()

        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, TimePickerFragment()).commit()
    }
}
