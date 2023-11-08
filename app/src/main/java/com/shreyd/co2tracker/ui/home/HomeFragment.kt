package com.shreyd.co2tracker.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.shreyd.co2tracker.Drive
import com.shreyd.co2tracker.Drive2
import com.shreyd.co2tracker.DriveAdapter
import com.shreyd.co2tracker.R
import com.shreyd.co2tracker.databinding.FragmentHomeBinding
import kotlin.properties.Delegates

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var savedEms: TextView? = null
    private var totalEms: TextView? = null
    private lateinit var authUser: FirebaseUser
    private lateinit var id: String
    private var numEnter = 0

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val drives = mutableListOf<Drive2>()
    lateinit var adapter: DriveAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        numEnter++

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        savedEms = _binding!!.co2emits
        totalEms = _binding!!.totalEmission

        authUser = Firebase.auth.currentUser!!
        var email = ""
        authUser.let{
            email = it.email!!
        }
        id = email.replace(".", "").replace("#", "")
            .replace("$", "").replace("[", "").replace("]", "")

        val dbUsers = FirebaseDatabase.getInstance().getReference("Users").child(id)

        dbUsers.child("Emissions").get().addOnSuccessListener {
            totalEms!!.text = it.value.toString()
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }

        dbUsers.child("Saved Emissions").get().addOnSuccessListener {
            savedEms!!.text = it.value.toString()
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }


        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Get Data From Firebase
        adapter = DriveAdapter(drives)
        binding.recycler.adapter = adapter


        if(numEnter == 1) {
            val dbUserDrives = FirebaseDatabase.getInstance().getReference("Users").child(id).child("Drives")
            var change = 0
            val driveListener = object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.e("error", "Success")
                    change ++
                    if(change == 1) {
                        val threshold = snapshot.childrenCount - 5
                        var countD = 0L
                        for(ds in snapshot.children) {
                            countD++
                            if (countD > threshold) {
                                val drive = ds.getValue(Drive2::class.java)
                                drives.add(drive!!)
                                println(drive.startTime)
                                println("-----------SIZE ${drives.size}--------------")
                                println(countD)

                                adapter.notifyDataSetChanged()
                            }

                        }
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("error", error.details)
                    println(error)
                }

            }
            dbUserDrives.addValueEventListener(driveListener)
        }



    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}