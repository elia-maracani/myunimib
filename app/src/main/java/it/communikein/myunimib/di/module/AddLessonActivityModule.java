package it.communikein.myunimib.di.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import it.communikein.myunimib.ui.timetable.AddLessonActivity;

@Module
public abstract class AddLessonActivityModule {

    @ContributesAndroidInjector
    abstract AddLessonActivity contributeAddLessonActivity();

}
