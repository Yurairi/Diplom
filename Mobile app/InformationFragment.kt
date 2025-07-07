// InformationFragment.kt
package com.example.smartflowusiassistant

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar



class InformationFragment : Fragment() {
    private var fragmentListener: FragmentInteractionListener? = null
    private var isFragmentVisible = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_information, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbarInformation)
        toolbar.title = "Справочная информация"
        toolbar.setNavigationIcon(R.drawable.ic_back_white)
        toolbar.setNavigationOnClickListener {
            Log.d("MainActivity", "Before setting isFragmentVisible to false: $isFragmentVisible")
            isFragmentVisible = false
            fragmentListener?.onFragmentVisibilityChanged(isFragmentVisible)
            Log.d("MainActivity", "After setting isFragmentVisible to false: $isFragmentVisible")
            navigateToBackStack()
        }
    }

    private fun navigateToBackStack() {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .remove(this)
            .commit()
        requireActivity().supportFragmentManager.popBackStack()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentInteractionListener) {
            fragmentListener = context
            isFragmentVisible = (context as MainActivity).isFragmentVisible
        } else {
            throw ClassCastException("$context должен реализовывать FragmentInteractionListener")
        }
    }
}