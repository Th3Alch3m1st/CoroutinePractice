package com.learn.coroutinepractice

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.learn.coroutinepractice.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class MainActivity : AppCompatActivity() {
    companion object {
        const val PROGRESS_MAX = 100
        const val PROGRESS_MIN = 0
        const val EXECUTION_TIME = 4000L
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var job: CompletableJob
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initClickListener()
    }

    private fun initClickListener() {
        with(binding) {
            btnBasicCoroutine.setOnClickListener {
                lifecycle.coroutineScope.launch(IO) {
                    testBasicCoroutine()
                }
            }
            btnJobPractice.setOnClickListener {
                if (!::job.isInitialized) {
                    initJob()
                    binding.progressbar.max = PROGRESS_MAX
                }

                binding.progressbar.setProgressWithJob(job)
            }

            btnAsyncAndWait.setOnClickListener {
                asyncAndAwait()
            }
        }
    }


    /**
     * withContext are used to switch between thread
     */
    private suspend fun testBasicCoroutine() {
        val result = fakeNetworkCall()
        withContext(Main) {
            binding.tvBasicCoroutine.text = result
        }
    }

    /**
     * withTimeoutOrNull bind job with time period, here it will return null since fake api call will need more than 1000ms
     */
    private suspend fun coroutineTimeoutExample() {
        val job = withTimeoutOrNull(1000) {
            val result = fakeNetworkCall()
        }
        if (job == null) {
            Log.e("error", "Timeout")
        }
    }

    private suspend fun fakeNetworkCall(): String {
        delay(1500)
        return "Hello from Fake API Call"
    }

    private fun initJob() {
        job = Job()
        job.invokeOnCompletion { throwable ->
            throwable?.let {
                val msg = throwable.message ?: "Unknown Exception"
                showToast(msg)
            }
        }
    }


    private fun LinearProgressIndicator.setProgressWithJob(job: Job) {

        if (binding.progressbar.progress > 0) {
            this.progress = 0
            resetJob()
            showToast("Resetting Job")
        } else {

            lifecycle.coroutineScope.launch(IO + job) {
                setButtonText("Cancel Job")
                for (i in PROGRESS_MIN..PROGRESS_MAX) {
                    delay(EXECUTION_TIME / PROGRESS_MAX)
                    withContext(Main) {
                        binding.progressbar.progress = i
                    }
                }
                showToast("Job Completed")
                setButtonText("Reset Job")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun resetJob() {
        if (job.isActive) {
            job.cancel(CancellationException("Job Cancelled"))
        }
        initJob()
        setButtonText("Execute Job")
    }

    private fun showToast(msg: String) {
        lifecycle.coroutineScope.launch(Main) {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setButtonText(text: String) {
        lifecycle.coroutineScope.launch(Main) {
            binding.btnJobPractice.text = text
        }
    }

    @SuppressLint("SetTextI18n")
    private fun asyncAndAwait() {
        lifecycle.coroutineScope.launch(IO) {
            val incrementSum: Deferred<Int> = async {
                getIncrementSum()
            }
            val decrementSum: Deferred<Int> = async {
                getDecrementSum()
            }
            val totalSum = incrementSum.await() + decrementSum.await()
            withContext(Main) {
                binding.tvTotalSum.text = "Total Sum : $totalSum"
            }
        }
    }

    private suspend fun getIncrementSum(): Int {
        var sum = 0
        for (i in 1..100) {
            sum += i
            delay(100)
            withContext(Main) {
                binding.tvIncrement.text = "$i"
            }
        }
        return sum
    }

    private suspend fun getDecrementSum(): Int {
        var sum = 0
        for (i in 200 downTo 150) {
            sum += i
            delay(100)
            withContext(Main) {
                binding.tvDecrement.text = "$i"
            }
        }
        return sum
    }
}