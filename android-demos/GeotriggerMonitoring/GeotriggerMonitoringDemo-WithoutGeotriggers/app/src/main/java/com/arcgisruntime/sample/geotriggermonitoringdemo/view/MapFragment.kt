package com.arcgisruntime.sample.geotriggermonitoringdemo.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.arcgisruntime.sample.geotriggermonitoringdemo.databinding.MapFragmentBinding
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class MapFragment : Fragment() {

    companion object {
        fun newInstance() = MapFragment()
    }

    private var _binding: MapFragmentBinding? = null
    private val binding get() = _binding!!

    private val mViewModel: MapViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // data binding no longer works with kotlin synthetic.
        // instead this binding class is generated, which we inflate, and then we return the binding's view
        _binding = MapFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.apply {
            map = mViewModel.map
            graphicsOverlays.add(mViewModel.graphicsOverlay)
            onTouchListener =
                object : DefaultMapViewOnTouchListener(context, binding.mapView) {
                    @SuppressLint("ClickableViewAccessibility")
                    override fun onLongPress(e: MotionEvent) {
                        val mapPoint = binding.mapView.screenToLocation(
                            android.graphics.Point(
                                e.x.roundToInt(),
                                e.y.roundToInt()
                            )
                        )
                        mViewModel.createPointOfInterest(mapPoint)
                    }
                }
        }
        mViewModel.viewpoint.observe(viewLifecycleOwner) { viewpoint ->
            binding.mapView.setViewpointAsync(viewpoint)
        }

        binding.clearButton.setOnClickListener { mViewModel.clearPointsOfInterest() }
        binding.edinburghButton.setOnClickListener { mViewModel.setEdinburgh() }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.dispose()
        // we have to destroy the binding here.
        _binding = null
    }
}