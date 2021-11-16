package com.arcgisruntime.sample.geotriggermonitoringdemo.di

import android.content.Context
import androidx.room.Room
import com.arcgisruntime.sample.geotriggermonitoringdemo.model.MapDatabase
import com.arcgisruntime.sample.geotriggermonitoringdemo.model.PointOfInterestDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    fun providePointOfInterestDao(mapDatabase: MapDatabase): PointOfInterestDao {
        return mapDatabase.pointOfInterestDao()
    }

    @Provides
    @Singleton
    fun provideArcGISMapDatabase(@ApplicationContext appContext: Context): MapDatabase {
        return Room.databaseBuilder(appContext, MapDatabase::class.java, "ArcGISMap Database")
            .fallbackToDestructiveMigration().build()
    }
}