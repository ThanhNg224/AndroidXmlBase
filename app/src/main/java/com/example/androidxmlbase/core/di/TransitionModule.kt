package com.example.androidxmlbase.core.di

import com.example.androidxmlbase.core.ui.transition.LanguageTransitionAction
import com.example.androidxmlbase.core.ui.transition.TransitionAction
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
abstract class TransitionModule {
    @Binds
    @IntoMap
    @StringKey(LanguageTransitionAction.KEY)
    abstract fun bindLanguageTransitionAction(impl: LanguageTransitionAction): TransitionAction
}
