package org.thoughtcrime.securesms.onboarding.recoverypassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import network.loki.messenger.R
import org.session.libsession.utilities.TextSecurePreferences
import org.session.libsignal.crypto.MnemonicCodec
import org.session.libsignal.utilities.hexEncodedPrivateKey
import org.thoughtcrime.securesms.BaseActionBarActivity
import org.thoughtcrime.securesms.crypto.IdentityKeyUtil
import org.thoughtcrime.securesms.crypto.MnemonicUtilities
import org.thoughtcrime.securesms.ui.AppTheme
import org.thoughtcrime.securesms.ui.Cell
import org.thoughtcrime.securesms.ui.CellNoMargin
import org.thoughtcrime.securesms.ui.CellWithPaddingAndMargin
import org.thoughtcrime.securesms.ui.LocalExtraColors
import org.thoughtcrime.securesms.ui.OutlineButton
import org.thoughtcrime.securesms.ui.PreviewTheme
import org.thoughtcrime.securesms.ui.SessionShieldIcon
import org.thoughtcrime.securesms.ui.ThemeResPreviewParameterProvider
import org.thoughtcrime.securesms.ui.classicDarkColors
import org.thoughtcrime.securesms.ui.colorDestructive
import org.thoughtcrime.securesms.ui.extraSmall
import org.thoughtcrime.securesms.ui.h8
import org.thoughtcrime.securesms.ui.small

class RecoveryPasswordActivity : BaseActionBarActivity() {

    private val viewModel: RecoveryPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.title = resources.getString(R.string.activity_recovery_password)

        ComposeView(this).apply {
            setContent {
                RecoveryPassword(viewModel.seed, viewModel.bitmap) { copySeed() }
            }
        }.let(::setContentView)
    }

    private fun revealSeed() {
        TextSecurePreferences.setHasViewedSeed(this, true)
    }

    private fun copySeed() {
        revealSeed()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", viewModel.seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}

@Preview
@Composable
fun PreviewMessageDetails(
    @PreviewParameter(ThemeResPreviewParameterProvider::class) themeResId: Int
) {
    PreviewTheme(themeResId) {
        RecoveryPassword(seed = "Voyage  urban  toyed  maverick peculiar tuxedo penguin tree grass building listen speak withdraw terminal plane")
    }
}

@Composable
fun RecoveryPassword(seed: String = "", bitmap: Bitmap? = null, copySeed:() -> Unit = {}) {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            RecoveryPasswordCell(seed, bitmap, copySeed)
            HideRecoveryPasswordCell()
        }
    }
}

@Composable
fun RecoveryPasswordCell(seed: String = "", bitmap: Bitmap? = null, copySeed:() -> Unit = {}) {
    val showQr = remember {
        mutableStateOf(false)
    }

    CellWithPaddingAndMargin {
        Column {
            Row {
                Text("Recovery Password")
                Spacer(Modifier.width(8.dp))
                SessionShieldIcon()
            }

            Text("Use your recovery password to load your account on new devices.\n\nYour account cannot be recovered without your recovery password. Make sure it's stored somewhere safe and secure — and don't share it with anyone.")

            AnimatedVisibility(!showQr.value) {
                Text(
                    seed,
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .border(
                            width = 1.dp,
                            color = classicDarkColors[3],
                            shape = RoundedCornerShape(11.dp)
                        )
                        .padding(24.dp),
                    style = MaterialTheme.typography.small.copy(fontFamily = FontFamily.Monospace),
                    color = LocalExtraColors.current.prominentButtonColor,
                )
            }

            AnimatedVisibility(showQr.value, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Card(
                    backgroundColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 24.dp)
                ) {
                    Box {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "some useful description",
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.session_shield),
                            contentDescription = "",
                            tint = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                                .width(46.dp)
                                .height(56.dp)
                                .background(color = Color.White)
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(!showQr.value) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    OutlineButton(text = stringResource(R.string.copy), modifier = Modifier.weight(1f), color = MaterialTheme.colors.onPrimary) { copySeed() }
                    OutlineButton(text = "View QR", modifier = Modifier.weight(1f), color = MaterialTheme.colors.onPrimary) { showQr.toggle() }
                }
            }

            AnimatedVisibility(showQr.value, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                OutlineButton(
                    text = "View Password",
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { showQr.toggle() }
            }
        }
    }
}

private fun MutableState<Boolean>.toggle() { value = !value }

@Composable
fun HideRecoveryPasswordCell() {
    CellWithPaddingAndMargin {
        Row {
            Column(Modifier.weight(1f)) {
                Text(text = "Hide Recovery Password", style = MaterialTheme.typography.h8)
                Text(text = "Permanently hide your recovery password on this device.")
            }
            OutlineButton(
                "Hide",
                modifier = Modifier.align(Alignment.CenterVertically),
                color = colorDestructive
            ) {}
        }
    }
}

fun Context.startRecoveryPasswordActivity() {
    Intent(this, RecoveryPasswordActivity::class.java).also(::startActivity)
}
