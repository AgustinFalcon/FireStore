package com.example.firestore

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.firestore.data.Person
import com.example.firestore.databinding.FragmentMainBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.Exception


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!


    private val personCollectionRef = Firebase.firestore.collection("persons")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.inflate(inflater, container, false)


        binding.btnUploadData.setOnClickListener {
            val person = getOldPerson();
            savePerson(person);
        }

        //Observer the changes into firestore in real time
        //subscribeToRealtimeUpdates();


        //Filter data press this button
        binding.btnRetrieveData.setOnClickListener {
            retrievePerson();
        }


        binding.btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson();
            val newPersonMap = getNewPersonMap();
            updatePerson(oldPerson = oldPerson, newPersonMap = newPersonMap)
        }


        binding.btnDeletePerson.setOnClickListener {
            val person = getOldPerson();
            deletePerson(person)
        }


        return binding.root
    }

    private fun getOldPerson() : Person{
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val age = binding.etAge.text.toString().toInt()

        return Person(firstName, lastName, age)
    }


    private fun getNewPersonMap(): Map<String, Any>{
        val firstName = binding.etNewFirstName.text.toString()
        val lastName = binding.etNewLastName.text.toString()
        val age = binding.etNewAge.text.toString()                //If the age don't change and you try convert a String empty to Int will be crash
        val map = mutableMapOf<String, Any>()

        if(firstName.isNotEmpty()){
            map["firstName"] = firstName
        }
        if(lastName.isNotEmpty()){
            map["lastName"] = lastName
        }
        if(age.isNotEmpty()){
            map["age"] = age.toInt()
        }

        return map
    }


    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()

        if(personQuery.documents.isNotEmpty()){
            for(document in personQuery){
                try{
                    personCollectionRef.document(document.id).delete().await()
                    //delete a specific person
                    /*personCollectionRef.document(document.id).update(mapOf(
                        "firstName" to FieldValue.delete()
                    ))*/

                }catch(e: Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }else{
            withContext(Dispatchers.Main){
                Toast.makeText(context, "No person matched the query", Toast.LENGTH_LONG).show()
            }
        }

    }


    private fun updatePerson(oldPerson: Person, newPersonMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", oldPerson.firstName)
            .whereEqualTo("lastName", oldPerson.lastName)
            .whereEqualTo("age", oldPerson.age)
            .get()
            .await()

        if(personQuery.documents.isNotEmpty()){
            for(document in personQuery){
                try {
                    personCollectionRef.document(document.id).set(
                        newPersonMap,
                        SetOptions.merge()
                    ).await()

                }catch(e: Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

        }else{
            withContext(Dispatchers.Main){
                Toast.makeText(context, "No person matched the query", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun subscribeToRealtimeUpdates(){
        personCollectionRef.addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
            //Any exception
            firebaseFirestoreException?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            //get data in real time
            querySnapshot?.let {
                val sb = StringBuilder()
                for(document in it){
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                binding.tvPersons.text = sb.toString()
            }
        }
    }



    private fun retrievePerson() = CoroutineScope(Dispatchers.IO).launch {
        val fromAge = binding.etFrom.text.toString().toInt()
        val toAge = binding.etTo.text.toString().toInt()

        try {
            //val querySnapshot = personCollectionRef.get().await()         for obtein all data

            val querySnapshot = personCollectionRef
                .whereGreaterThan("age", fromAge)                       //Filter data beetween
                .whereLessThan("age", toAge)
                .orderBy("age")
                .get()
                .await()

            val sb = StringBuilder()

            for(document in querySnapshot.documents){
                val person = document.toObject<Person>(/*Person::class.java*/)
                sb.append("$person\n")
            }

            withContext(Dispatchers.Main){
                binding.tvPersons.text = sb.toString()
            }
        }catch(e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try{

            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main){
                Toast.makeText(context, "Successfully saved data", Toast.LENGTH_LONG).show()
            }

        }catch(e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

}