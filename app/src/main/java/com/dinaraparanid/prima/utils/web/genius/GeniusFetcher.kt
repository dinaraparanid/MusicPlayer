package com.dinaraparanid.prima.utils.web.genius

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dinaraparanid.prima.utils.web.genius.search_response.SearchResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Searches for track info.
 * Makes GET call to found all matches of track with their data
 */

class GeniusFetcher {
    private val geniusApi = Retrofit.Builder()
        .baseUrl("https://api.genius.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeniusApi::class.java)

    /**
     * Fetches track's data from Genius
     * @param search title and artist to search
     * @return search response
     */

    internal fun fetchTrackDataSearch(search: String): LiveData<SearchResponse> {
        val responseLiveData = MutableLiveData<SearchResponse>()

        geniusApi.fetchTrackDataSearch(search).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(
                call: Call<SearchResponse>,
                response: Response<SearchResponse>
            ) {
                responseLiveData.value = response.body()
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) = Unit
        })

        return responseLiveData
    }
}