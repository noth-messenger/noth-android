package org.thoughtcrime.securesms.conversation.expiration

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import network.loki.messenger.BuildConfig
import network.loki.messenger.R
import network.loki.messenger.databinding.ActivityExpirationSettingsBinding
import network.loki.messenger.libsession_util.util.ExpiryMode
import org.session.libsession.messaging.messages.ExpirationConfiguration
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity
import org.thoughtcrime.securesms.database.RecipientDatabase
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.preferences.ExpirationRadioOption
import org.thoughtcrime.securesms.preferences.RadioOptionAdapter
import org.thoughtcrime.securesms.util.ConfigurationMessageUtilities
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class ExpirationSettingsActivity: PassphraseRequiredActionBarActivity() {

    private lateinit var binding : ActivityExpirationSettingsBinding

    @Inject lateinit var recipientDb: RecipientDatabase
    @Inject lateinit var threadDb: ThreadDatabase
    @Inject lateinit var viewModelFactory: ExpirationSettingsViewModel.AssistedFactory

    private val threadId: Long by lazy {
        intent.getLongExtra(THREAD_ID, -1)
    }

    private val viewModel: ExpirationSettingsViewModel by viewModels {
        val afterReadOptions = resources.getIntArray(R.array.read_expiration_time_values).map(Int::toString)
            .zip(resources.getStringArray(R.array.read_expiration_time_names)) { value, name ->
                ExpirationRadioOption(ExpiryMode.AfterRead(value.toLong()), name, getString(R.string.AccessibilityId_time_option))
            }
        val afterSendOptions = resources.getIntArray(R.array.send_expiration_time_values).map(Int::toString)
            .zip(resources.getStringArray(R.array.send_expiration_time_names)) { value, name ->
                ExpirationRadioOption(ExpiryMode.AfterSend(value.toLong()), name, getString(R.string.AccessibilityId_time_option))
            }
        viewModelFactory.create(threadId, mayAddTestExpiryOption(afterReadOptions), mayAddTestExpiryOption(afterSendOptions))
    }

    private fun mayAddTestExpiryOption(expiryOptions: List<ExpirationRadioOption>): List<ExpirationRadioOption> {
        return if (BuildConfig.DEBUG) {
            val options = expiryOptions.toMutableList()
            val added = when (options.first().value) {
                is ExpiryMode.AfterRead -> ExpiryMode.AfterRead(60)
                is ExpiryMode.AfterSend -> ExpiryMode.AfterSend(60)
                is ExpiryMode.Legacy -> ExpiryMode.Legacy(60)
                ExpiryMode.NONE -> ExpiryMode.NONE // shouldn't happen
            }
            options.add(1, ExpirationRadioOption(added, "1 Minute (for testing purposes)"))
            options
        } else expiryOptions
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val scrollParcelArray = SparseArray<Parcelable>()
        binding.scrollView.saveHierarchyState(scrollParcelArray)
        outState.putSparseParcelableArray(SCROLL_PARCEL, scrollParcelArray)
    }

    override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
        super.onCreate(savedInstanceState, ready)
        binding = ActivityExpirationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpToolbar()

        savedInstanceState?.let { bundle ->
            val scrollStateParcel = bundle.getSparseParcelableArray<Parcelable>(SCROLL_PARCEL)
            if (scrollStateParcel != null) {
                binding.scrollView.restoreHierarchyState(scrollStateParcel)
            }
        }

        val deleteTypeOptions = getDeleteOptions()
        val deleteTypeOptionAdapter = RadioOptionAdapter {
            viewModel.onExpirationTypeSelected(it)
        }
        binding.recyclerViewDeleteTypes.apply {
            adapter = deleteTypeOptionAdapter
            addItemDecoration(ContextCompat.getDrawable(this@ExpirationSettingsActivity, R.drawable.conversation_menu_divider)!!.let {
                DividerItemDecoration(this@ExpirationSettingsActivity, RecyclerView.VERTICAL).apply {
                    setDrawable(it)
                }
            })
            setHasFixedSize(true)
        }
        deleteTypeOptionAdapter.submitList(deleteTypeOptions)

        val timerOptionAdapter = RadioOptionAdapter {
            viewModel.onExpirationTimerSelected(it)
        }
        binding.recyclerViewTimerOptions.apply {
            adapter = timerOptionAdapter
            addItemDecoration(ContextCompat.getDrawable(this@ExpirationSettingsActivity, R.drawable.conversation_menu_divider)!!.let {
                DividerItemDecoration(this@ExpirationSettingsActivity, RecyclerView.VERTICAL).apply {
                    setDrawable(it)
                }
            })
        }
        binding.buttonSet.setOnClickListener {
            viewModel.onSetClick()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState.settingsSaved) {
                        true -> {
                            ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@ExpirationSettingsActivity)
                            finish()
                        }
                        false -> showToast(getString(R.string.ExpirationSettingsActivity_settings_not_updated))
                        else -> {}
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedExpirationType.collect { type ->
                    val position = deleteTypeOptions.indexOfFirst { it.value == type }
                    deleteTypeOptionAdapter.setSelectedPosition(max(0, position))
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedExpirationTimer.collect { option ->
                    val position =
                        viewModel.expirationTimerOptions.value.indexOfFirst { it.value == option?.value }
                    timerOptionAdapter.setSelectedPosition(max(0, position))
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expirationTimerOptions.collect { options ->
                    binding.textViewTimer.isVisible =
                        options.isNotEmpty() && viewModel.uiState.value.showExpirationTypeSelector
                    binding.layoutTimer.isVisible = options.isNotEmpty()
                    timerOptionAdapter.submitList(options)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipient.collect {
                    binding.textViewDeleteType.isVisible = viewModel.uiState.value.showExpirationTypeSelector
                    binding.layoutDeleteTypes.isVisible = viewModel.uiState.value.showExpirationTypeSelector
                    binding.textViewFooter.isVisible = it?.isClosedGroupRecipient == true
                    binding.textViewFooter.text = HtmlCompat.fromHtml(getString(R.string.activity_expiration_settings_group_footer), HtmlCompat.FROM_HTML_MODE_COMPACT)
                }
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getDeleteOptions(): List<ExpirationRadioOption> {
        if (!viewModel.uiState.value.showExpirationTypeSelector) return emptyList()

        val deleteTypeOptions = mutableListOf<ExpirationRadioOption>()
        if (ExpirationConfiguration.isNewConfigEnabled) {
            if (viewModel.recipient.value?.isContactRecipient == true && viewModel.recipient.value?.isLocalNumber == false) {
                deleteTypeOptions.addAll(
                    listOf(
                        ExpirationRadioOption(
                            value = ExpiryMode.NONE,
                            title = getString(R.string.expiration_off),
                            contentDescription = getString(R.string.AccessibilityId_disable_disappearing_messages)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterRead(0),
                            title = getString(R.string.expiration_type_disappear_after_read),
                            subtitle = getString(R.string.expiration_type_disappear_after_read_description),
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_read_option)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterSend(0),
                            title = getString(R.string.expiration_type_disappear_after_send),
                            subtitle = getString(R.string.expiration_type_disappear_after_send_description),
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_send_option)
                        )
                    )
                )
            } else if (viewModel.recipient.value?.isLocalNumber == true) {
                deleteTypeOptions.addAll(
                    listOf(
                        ExpirationRadioOption(
                            value = ExpiryMode.NONE,
                            title = getString(R.string.expiration_off),
                            contentDescription = getString(R.string.AccessibilityId_disable_disappearing_messages)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterSend(0),
                            title = getString(R.string.expiration_type_disappear_after_send),
                            subtitle = getString(R.string.expiration_type_disappear_after_send_description),
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_send_option)
                        )
                    )
                )
            } else if (viewModel.recipient.value?.isClosedGroupRecipient == true) {
                deleteTypeOptions.addAll(
                    listOf(
                        ExpirationRadioOption(
                            value = ExpiryMode.NONE,
                            title = getString(R.string.expiration_off),
                            contentDescription = getString(R.string.AccessibilityId_disable_disappearing_messages)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterSend(0),
                            title = getString(R.string.expiration_type_disappear_after_send),
                            subtitle = getString(R.string.expiration_type_disappear_after_send_description),
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_send_option)
                        )
                    )
                )
            }
        } else {
            if (viewModel.recipient.value?.isContactRecipient == true && viewModel.recipient.value?.isLocalNumber == false) {
                deleteTypeOptions.addAll(
                    listOf(
                        ExpirationRadioOption(
                            value = ExpiryMode.NONE,
                            title = getString(R.string.expiration_off),
                            contentDescription = getString(R.string.AccessibilityId_disable_disappearing_messages)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.Legacy(0),
                            title = getString(R.string.expiration_type_disappear_legacy),
                            subtitle = getString(R.string.expiration_type_disappear_legacy_description)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterRead(0),
                            title = getString(R.string.expiration_type_disappear_after_read),
                            subtitle = getString(R.string.expiration_type_disappear_after_read_description),
                            enabled = false,
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_read_option)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterSend(0),
                            title = getString(R.string.expiration_type_disappear_after_send),
                            subtitle = getString(R.string.expiration_type_disappear_after_send_description),
                            enabled = false,
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_send_option)
                        )
                    )
                )
            } else {
                deleteTypeOptions.addAll(
                    listOf(
                        ExpirationRadioOption(value = ExpiryMode.NONE, title = getString(R.string.expiration_off)),
                        ExpirationRadioOption(
                            value = ExpiryMode.Legacy(0),
                            title = getString(R.string.expiration_type_disappear_legacy),
                            subtitle = getString(R.string.expiration_type_disappear_legacy_description)
                        ),
                        ExpirationRadioOption(
                            value = ExpiryMode.AfterSend(0),
                            title = getString(R.string.expiration_type_disappear_after_send),
                            subtitle = getString(R.string.expiration_type_disappear_after_send_description),
                            enabled = false,
                            contentDescription = getString(R.string.AccessibilityId_disappear_after_send_option)
                        )
                    )
                )
            }
        }
        return deleteTypeOptions
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar ?: return
        actionBar.title = getString(R.string.activity_expiration_settings_title)
        actionBar.subtitle = if (viewModel.selectedExpirationType.value is ExpiryMode.AfterSend) {
            getString(R.string.activity_expiration_settings_subtitle_sent)
        } else {
            getString(R.string.activity_expiration_settings_subtitle)
        }
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(true)
    }

    companion object {
        private const val SCROLL_PARCEL = "scroll_parcel"
        const val THREAD_ID = "thread_id"
    }

}