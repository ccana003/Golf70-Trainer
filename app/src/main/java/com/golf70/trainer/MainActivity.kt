package com.golf70.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.ui.navigation.Dependencies
import com.golf70.trainer.ui.navigation.Golf70NavHost
import com.golf70.trainer.ui.navigation.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = Dependencies.repository(applicationContext)
        setContent {
            MaterialTheme {
                Surface {
                    val vm: MainViewModel = viewModel(factory = MainViewModel.factory(repository))
                    Golf70NavHost(vm = vm)
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewApp() {
    MaterialTheme { Surface {} }
}
