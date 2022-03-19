package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.os.ConditionVariable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentViewPagerBinding
import com.dinaraparanid.prima.fragments.main_menu.UltimateCollectionFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** Ancestor for all View Pager Fragments */

abstract class ViewPagerFragment : MainActivitySimpleFragment<FragmentViewPagerBinding>() {
    final override var binding: FragmentViewPagerBinding? = null

    protected abstract val fragmentsConstructors: Array<() -> Fragment>
    protected abstract val fragmentsTitles: IntArray
    protected abstract val isTabShown: Boolean

    private var startSelectedType = 0

    internal companion object {
        private const val ARG_SELECTED_TYPE = "selected_type"

        /**
         * Creates new instance of [ViewPagerFragment] with given arguments
         * @param selectedType type of fragment
         * (from 0 to number of fragments that this fragment contains, exclusive)
         */

        @JvmStatic
        internal fun <T : ViewPagerFragment> newInstance(
            selectedType: Int,
            clazz: KClass<T>,
        ) = clazz.constructors.first().call().apply {
            arguments = Bundle().apply { putInt(ARG_SELECTED_TYPE, selectedType) }
        }
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = ""
        startSelectedType = requireArguments().getInt(ARG_SELECTED_TYPE)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentViewPagerBinding>(
            inflater,
            R.layout.fragment_view_pager,
            container,
            false
        ).apply {
            viewModel = ViewModel()
            tabVisibility = if (this@ViewPagerFragment.isTabShown) View.VISIBLE else View.GONE
        }

        return binding!!.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.pager.run {
            adapter = FavouritesAdapter()

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        fragmentActivity.currentFragment.unchecked.setHasOptionsMenu(true)
                        requireActivity().invalidateOptionsMenu()
                    }
                }
            })
        }

        fragmentActivity.runOnUIThread {
            delay(100)
            binding!!.pager.setCurrentItem(startSelectedType, false)

            launch(Dispatchers.Default) {
                val initFirstFragmentCondVar = ConditionVariable()

                loop@ while (true) {
                    when {
                        fragmentActivity.currentFragment.get() !is ViewPagerFragment -> {
                            fragmentActivity.currentFragment.unchecked.setHasOptionsMenu(true)
                            break@loop
                        }

                        else -> initFirstFragmentCondVar.block(100)
                    }

                    Exception(fragmentActivity.currentFragment.get().toString()).printStackTrace()
                }
            }

            //fragmentActivity.currentFragment.unchecked.setHasOptionsMenu(true)
        }

        if (isTabShown) TabLayoutMediator(binding!!.tabLayout, binding!!.pager) { tab, pos ->
            tab.text = resources.getString(fragmentsTitles[pos])
        }.attach()
    }

    private inner class FavouritesAdapter : FragmentStateAdapter(
        childFragmentManager,
        viewLifecycleOwner.lifecycle
    ) {
        override fun getItemCount() = fragmentsConstructors.size
        override fun createFragment(position: Int) = fragmentsConstructors[position]()
    }
}