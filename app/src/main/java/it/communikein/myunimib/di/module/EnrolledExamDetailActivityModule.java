package it.communikein.myunimib.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import it.communikein.myunimib.ui.detail.EnrolledExamDetailActivity;

@Module
public abstract class EnrolledExamDetailActivityModule {

    @ContributesAndroidInjector
    abstract EnrolledExamDetailActivity contributeEnrolledExamDetailActivity();

}
