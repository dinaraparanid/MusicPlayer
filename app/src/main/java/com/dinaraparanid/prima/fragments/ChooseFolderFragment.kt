package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Folder
import com.dinaraparanid.prima.databinding.FragmentChooseFolderBinding
import com.dinaraparanid.prima.databinding.ListItemFolderBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.AsyncListDifferAdapter
import com.dinaraparanid.prima.utils.polymorphism.fragments.CallbacksFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.MenuProviderFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.UpdatingListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.defaultMenuProvider
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ChooseFolderViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import java.lang.ref.WeakReference

class ChooseFolderFragment :
    UpdatingListFragment<FoldersActivity,
            Folder,
            ChooseFolderFragment.FolderAdapter,
            ChooseFolderFragment.FolderAdapter.FolderHolder,
            FragmentChooseFolderBinding>(),
    MenuProviderFragment {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Saves [folder]'s [Folder.path] as path of converted mp3 tracks
         * @param folder folder which [Folder.path] will be saved
         */

        fun onFolderSelected(folder: Folder)
    }

    private lateinit var folder: Folder
    private var awaitDialog: KProgressHUD? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override var _adapter: FolderAdapter? = null
    override var binding: FragmentChooseFolderBinding? = null
    override var emptyTextView: TextView? = null
    override var updater: SwipeRefreshLayout? = null
    override val menuProvider = defaultMenuProvider

    internal companion object {
        private const val FOLDER_KEY = "folder"

        /**
         * Creates new instance of [ChooseFolderFragment] with given param
         * @param folder [Folder] which sub folders will be shown
         * @return created fragment
         */

        @JvmStatic
        internal fun newInstance(folder: Folder) = ChooseFolderFragment().apply {
            arguments = Bundle().apply {
                putSerializable(FOLDER_KEY, folder)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        folder = requireArguments().getSerializable(FOLDER_KEY) as Folder
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentChooseFolderBinding>(
                inflater,
                R.layout.fragment_choose_folder,
                container,
                false
            )
            .apply {
                viewModel = ChooseFolderViewModel(
                    folder.path,
                    WeakReference(this@ChooseFolderFragment)
                )

                emptyTextView = foldersEmpty

                updater = foldersSwipeRefreshLayout.apply {
                    setOnRefreshListener {
                        runOnUIThread {
                            setColorSchemeColors(Params.getInstanceSynchronized().primaryColor)
                            loadAsync().join()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }
            }

        runOnUIThread {
            val task = loadAsync()
            awaitDialog = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            awaitDialog?.dismiss()
            initAdapter()

            itemListSearch.addAll(itemList)
            adapter.setCurrentList(itemList)
            setEmptyTextViewVisibility(itemList)

            recyclerView = binding!!.foldersRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@ChooseFolderFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }
        }

        return binding!!.root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@ChooseFolderFragment)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }

    override suspend fun loadAsync() = runOnWorkerThread {
        itemList.clear()
        itemList.addAll(folder.folders)
    }

    /** Filters folders with [query] (folder's title must contains [query]) */
    override fun filter(models: Collection<Folder>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    /** Updates UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: List<Folder>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    /** Initializes adapter */
    override fun initAdapter() {
        _adapter = FolderAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** [RecyclerView.Adapter] for [ChooseFolderFragment] */
    inner class FolderAdapter : AsyncListDifferAdapter<Folder, FolderAdapter.FolderHolder>() {
        override fun areItemsEqual(first: Folder, second: Folder) = first == second

        /** [RecyclerView.ViewHolder] for folders of [FolderAdapter] */
        inner class FolderHolder(private val folderBinding: ListItemFolderBinding) :
            RecyclerView.ViewHolder(folderBinding.root),
            View.OnClickListener {
            private lateinit var folder: Folder

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) = (callbacker as Callbacks).onFolderSelected(folder)

            /**
             * Constructs GUI for folder item
             * @param _folder folder to bind
             */

            internal fun bind(_folder: Folder) {
                folderBinding.viewModel = binding!!.viewModel!!
                folderBinding.folder = _folder
                folderBinding.executePendingBindings()
                folder = _folder
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FolderHolder(
            ListItemFolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: FolderHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}