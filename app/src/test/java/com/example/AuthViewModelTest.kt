package com.example

import com.example.viewmodel.AuthStatus
import com.example.viewmodel.AuthUser
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authState transitions reactively between unauthenticated and authenticated`() = runTest(testDispatcher) {
        val fakeUserFlow = MutableStateFlow<AuthUser?>(null)
        val viewModel = AuthViewModel(authUserFlow = fakeUserFlow, initialUser = null)

        advanceUntilIdle()
        assertEquals(AuthStatus.Unauthenticated, viewModel.authState.value.status)
        assertFalse(viewModel.authState.value.isAuthenticated)
        assertNull(viewModel.authState.value.userId)

        val user = AuthUser(uid = "uid_123", displayName = "Aarav", email = "aarav@example.com")
        fakeUserFlow.value = user
        advanceUntilIdle()

        assertTrue(viewModel.authState.value.isAuthenticated)
        assertEquals("uid_123", viewModel.authState.value.userId)
        assertEquals(AuthStatus.Authenticated(user), viewModel.authState.value.status)

        fakeUserFlow.value = null
        advanceUntilIdle()

        assertEquals(AuthStatus.Unauthenticated, viewModel.authState.value.status)
        assertFalse(viewModel.authState.value.isAuthenticated)
    }
}
