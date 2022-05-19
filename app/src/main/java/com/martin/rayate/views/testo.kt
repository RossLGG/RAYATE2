package com.martin.rayate.views

import com.google.firebase.firestore.FirebaseFirestore
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.martin.rayate.views.testo
import com.google.android.gms.tasks.OnCompleteListener
import com.firebase.geofire.GeoQueryBounds
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import java.util.ArrayList
import java.util.HashMap

object testo {
    private val db = FirebaseFirestore.getInstance()
    fun addGeoHash() {
        // [START fs_geo_add_hash]
        // Compute the GeoHash for a lat/lng point
        val lat = 51.5074
        val lng = 0.1278
        val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(lat, lng))

        // Add the hash and the lat/lng to the document. We will use the hash
        // for queries and the lat/lng for distance comparisons.
        val updates: MutableMap<String, Any> = HashMap()
        updates["geohash"] = hash
        updates["lat"] = lat
        updates["lng"] = lng
        val londonRef = db.collection("cities").document("LON")
        londonRef.update(updates)
            .addOnCompleteListener {
                // ...
            }
        // [END fs_geo_add_hash]
    }

    fun queryHashes() {
        // [START fs_geo_query_hashes]
        // Find cities within 50km of London
        val center = GeoLocation(51.5074, 0.1278)
        val radiusInM = (50 * 1000).toDouble()

        // Each item in 'bounds' represents a startAt/endAt pair. We have to issue
        // a separate query for each pair. There can be up to 9 pairs of bounds
        // depending on overlap, but in most cases there are 4.
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = db.collection("cities")
                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        // Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap.documents) {
                        val lat = doc.getDouble("lat")!!
                        val lng = doc.getDouble("lng")!!

                        // We have to filter out a few false positives due to GeoHash
                        // accuracy, but most will match
                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                        if (distanceInM <= radiusInM) {
                            matchingDocs.add(doc)
                        }
                    }
                }

                // matchingDocs contains the results
                // ...
            }
        // [END fs_geo_query_hashes]
    }
}