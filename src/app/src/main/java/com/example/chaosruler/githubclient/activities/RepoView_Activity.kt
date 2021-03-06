package com.example.chaosruler.githubclient.activities

import android.annotation.SuppressLint
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.chaosruler.githubclient.R
import com.example.chaosruler.githubclient.fragments.fragments.Issues.Issues_fragment
import com.example.chaosruler.githubclient.fragments.fragments.Wiki_fragment
import com.example.chaosruler.githubclient.fragments.fragments.commits.commit_fragment
import com.example.chaosruler.githubclient.fragments.fragments.repo_files.repo_files_fragment
import com.example.chaosruler.githubclient.fragments.fragments.user_fragment.user_fragment
import com.example.chaosruler.githubclient.services.themer

import kotlinx.android.synthetic.main.activity_repo_view.*
import java.util.*


/**
 * a repoView activity, when viewing a repo (be it mine or queried), this view opens tab for that
 */
class RepoView_Activity : AppCompatActivity() {

    companion object {
        /**
         * this activity, for easier communication between fragments and activity (buggy a bit using .getActivity on kotlin)
         */
        @SuppressLint("StaticFieldLeak")
        var act: AppCompatActivity? = null
        /**
         * tts engine to read content when needed
         */
        var tts:TextToSpeech? = null

        /**
         * call for tts engine to read a string
         * @param string the string to read
         */
        @Suppress("unused")
        fun speakOut(string:String)
        {
            try
            {
                if (tts != null) {
                    if (tts!!.isSpeaking) {
                        tts!!.stop()
                    } else {
                        tts!!.speak(string, TextToSpeech.QUEUE_FLUSH, null, "")
                    }
                }
            }
            catch (e:Exception)
            {
                Log.d("TTS","Something went wrong")
            }
        }
    }
    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    /**
     * the repo name of the repo we want to view
     */
    private lateinit var repo_name:String
    /**
     * the username of the repo we want to view
     */
    private lateinit var user_name:String

    /**
     * on create, creates the logic of creating a tab view with multiple fragments set
     */
    @Suppress("UNUSED_VARIABLE")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(themer.style(baseContext))
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                // set US English as language for tts
                val result = tts!!.setLanguage(Locale.US)

            } else {
                Log.e("TTS", "Initilization Failed!")
            }
        })
        setContentView(R.layout.activity_repo_view)
        /*
            save this activiy as a variable, kotlin cut the activity access
            and I would want error handling using that activity instance
         */
        act = this
        /*
            attempts to load the arguements (reponame, username) of the repo I want to show
         */
        try
        {
            repo_name = intent.getStringExtra(getString(R.string.repo_name_key))
            user_name = intent.getStringExtra(getString(R.string.user_name_key))
        }
        catch (e:Exception)
        {
            /*
                must have those variables
             */
            finish()
        }

        /*
            set action bar, used for tableview
         */
        setSupportActionBar(repo_finder_toolbar)
        /*
            adds table view toolbar
         */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        /*
            hides toolbar, leaving tableview only
         */
        supportActionBar!!.hide()
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        /*
            amount of fragments before knowing for sure there's a wiki page is 2, third fragment is wiki display
         */
        val amount = 5
        Thread{

            /*
                   connects tableview to sectinPagerAdapter, with a custom one we made to handle fragments
             */
            runOnUiThread {  /*
                  case we don't have a wiki, therefore only 2 fragments
                  we remove the last tab, since we won't be using it - there's no fragment to show there if there isn't a wiki
               */
                mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, amount)
                /*
                    sets activity "container" to get data from sectionPagerAdapter (aka, inflate the fragment there)
                 */
                repo_container.adapter = mSectionsPagerAdapter

                // Set up the ViewPager with the sections adapter.
                repo_container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(repo_finder_table))
                repo_finder_table.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(repo_container))
            }

        }.start()




    }




    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager,
                                     /**
                                      * the number of tabs we should open, each tab consists a fragment
                                      */
                                     private var amount: Int) : FragmentPagerAdapter(fm)
    {
        /**
         * gets a fragment by id for tab by position
         * @param position the position of the fragment that we want to get
         */
        override fun getItem(position: Int): Fragment = when(position)
        {
            /*
                first fragment is repo-data showing satiitcs, second fragment is repo files, showing repo files and directries
                third fragment is a wiki fragment, opening a fragment with HTML view
             */
            0->com.example.chaosruler.githubclient.fragments.fragments.repo_data.newInstance(baseContext,user_name,repo_name)
            1->Issues_fragment.newInstance(baseContext,user_name,repo_name)
            2-> commit_fragment.newInstance(baseContext,user_name,repo_name)
            3->repo_files_fragment.newInstance(baseContext,user_name,repo_name)
            4->Wiki_fragment.newInstance(baseContext,user_name,repo_name)
            else-> user_fragment.newInstance()
        }

        /**
         * have to implement this, gets the amount of tabs available
         */
        override fun getCount(): Int {
            return amount
        }
    }
    /**
     * should close tts
     */
    public override fun onDestroy()
    {
        // Shutdown TTS
        if (tts != null)
        {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

}
