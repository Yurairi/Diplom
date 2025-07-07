package com.example.smartflowusiassistant

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import java.io.File
import android.provider.DocumentsContract



interface FragmentInteractionListener {
    fun onFragmentVisibilityChanged(isVisible: Boolean)
    fun onFileSavePathChanged(newPath: String)
}

class SettingsFragment : Fragment() {
    private val REQUEST_CODE = 42
    private lateinit var textFilePath: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var fragmentListener: FragmentInteractionListener? = null
    private var isFragmentVisible = false
    private val folderName = "DigitalAssistant"

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        textFilePath = view.findViewById(R.id.TextFilePath)
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val defaultPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
        var defaultPathString = defaultPath.absolutePath
        var savedPath = sharedPreferences.getString("selectedPath", defaultPathString)

        textFilePath.text = "Выбрать путь для сохранения файлов: ${savedPath}"
        textFilePath.setOnClickListener {
            openFileExplorer()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbarSettings)
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

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { treeUri ->
                val directoryPath = getPathFromTreeUri(treeUri)
                sharedPreferences.edit().putString("selectedPath", directoryPath).apply()
                textFilePath.text = "Путь для сохранения файлов: $directoryPath"
                fragmentListener?.onFileSavePathChanged(directoryPath!!)
            }
        }
    }

    private fun getPathFromTreeUri(treeUri: android.net.Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":").toTypedArray()
        val type = split[0]

        if ("primary".equals(type, ignoreCase = true)) {
            return "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
        } else {
            return null 
        }
    }
}
