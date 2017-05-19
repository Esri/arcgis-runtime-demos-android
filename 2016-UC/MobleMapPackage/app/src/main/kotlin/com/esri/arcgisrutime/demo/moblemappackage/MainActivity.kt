/* Copyright 2017 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgisrutime.demo.moblemappackage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.MobileMapPackage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    val TAG = "MMPK"
    val FILE_EXTENSION = ".mmpk"
    var extStorDir: File? = null
    var extSDCardDirName: String? = null
    var filename: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // get sdcard resource name
        extStorDir = Environment.getExternalStorageDirectory()
        // get the directory
        extSDCardDirName = this.resources.getString(R.string.config_data_sdcard_offline_dir)
        // get mobile map package filename
        filename = this.resources.getString(R.string.config_mmpk_name)
        // create the full path to the mobile map package file
        val mmpkFile = createMobileMapPackageFilePath()

        // For API level 23+ request permission at runtime
        val reqPermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val requestCode = 2
        if(ContextCompat.checkSelfPermission(this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED){
            loadMobileMapPackage(mmpkFile)
        }else{
            ActivityCompat.requestPermissions(this, reqPermission, requestCode)
        }
    }

    /**
     * Handle the permissions request response
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMobileMapPackage(mmpkFile)
        } else {
            // report to user that permission was denied
            Toast.makeText(this@MainActivity, resources.getString(R.string.location_permission_denied),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMobileMapPackage(mmpkFile: String){
        // create the mobile map package
        val mapPackage = MobileMapPackage(mmpkFile)
        // load the mobile map package asynchronously
        mapPackage.loadAsync()
        // add done listener which will invoke when mobile map package has loaded
        mapPackage.addDoneLoadingListener {
            // check load status and that the mobile map package has maps
            if (mapPackage.loadStatus == LoadStatus.LOADED && mapPackage.maps.size > 0) {
                // add the map from the mobile map package to the MapView
                mapView!!.map = mapPackage.maps[0]
            } else {
                // Log an issue if the mobile map package fails to load
                Log.e(TAG, mapPackage.loadError.message)
            }
        }
    }

    /**
     * Create the mobile map package file location and name structure
     */
    private fun createMobileMapPackageFilePath(): String {
        return extStorDir!!.absolutePath + File.separator + extSDCardDirName + File.separator + filename + FILE_EXTENSION
    }

    override fun onPause() {
        super.onPause()
        mapView!!.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.resume()
    }
}
