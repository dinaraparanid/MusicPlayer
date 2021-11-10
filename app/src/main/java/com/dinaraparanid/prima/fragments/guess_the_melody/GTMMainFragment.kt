package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentGuessTheMelodyMainBinding
import com.dinaraparanid.prima.utils.polymorphism.MainActivitySimpleFragment
import com.dinaraparanid.prima.viewmodels.mvvm.GuessTheGameMainViewModel
import java.lang.ref.WeakReference

/**
 * Fragment that starts "Guess the Melody" game
 */

class GTMMainFragment : MainActivitySimpleFragment<FragmentGuessTheMelodyMainBinding>() {
    override var binding: FragmentGuessTheMelodyMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.guess_the_melody)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGuessTheMelodyMainBinding>(
            inflater,
            R.layout.fragment_guess_the_melody_main,
            container,
            false
        ).apply {
            viewModel = GuessTheGameMainViewModel(
                WeakReference(this@GTMMainFragment),
                fragmentActivity.mainLabelCurText
            )
        }

        return binding!!.root
    }
}