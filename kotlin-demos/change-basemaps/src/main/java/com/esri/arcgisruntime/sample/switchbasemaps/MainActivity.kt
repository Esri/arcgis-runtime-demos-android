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

package com.esri.arcgisruntime.sample.switchbasemaps

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var map: ArcGISMap
    // The basemap switching menu items.
    lateinit var mStreetsMenuItem: MenuItem
    lateinit var mTopoMenuItem: MenuItem
    lateinit var mGrayMenuItem: MenuItem
    lateinit var mOceansMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // create a map with Topographic Basemap
        map = ArcGISMap(Basemap.Type.TOPOGRAPHIC, 47.605800, -122.315308, 12)
        // set the map to be displayed in this view
        mapView.map = map
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // Get the basemap switching menu items.
        mStreetsMenuItem = menu.getItem(0)
        mTopoMenuItem = menu.getItem(1)
        mGrayMenuItem = menu.getItem(2)
        mOceansMenuItem = menu.getItem(3)
        // set the topo menu item checked by default
        mTopoMenuItem.isChecked = true

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.World_Street_Map -> consume {
            // create a map with Streets Basemap
            map.basemap = Basemap.createStreets()
            mStreetsMenuItem.isChecked = true
        }
        R.id.World_Topo -> consume {
            // create a map with Topographic Basemap
            map.basemap = Basemap.createTopographic()
            mTopoMenuItem.isChecked = true
        }
        R.id.Gray -> consume{
            // create a map with Gray Basemap
            map.basemap = Basemap.createLightGrayCanvas()
            mGrayMenuItem.isChecked = true
        }
        R.id.Ocean_Basemap -> consume{
            // create a map with Oceans Basemap
            map.basemap = Basemap.createOceans()
            mOceansMenuItem.isChecked = true
        }
        else -> super.onOptionsItemSelected(item)
    }

    inline fun consume(f: () -> Unit): Boolean{
        f()
        return true
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }
}
