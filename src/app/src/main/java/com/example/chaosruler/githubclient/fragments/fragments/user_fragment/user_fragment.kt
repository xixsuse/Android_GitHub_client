package com.example.chaosruler.githubclient.fragments.fragments.user_fragment



import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.ProgressBar
import com.example.chaosruler.githubclient.R
import com.example.chaosruler.githubclient.activities.MainActivity
import com.example.chaosruler.githubclient.services.GitHub_remote_service
import com.example.chaosruler.githubclient.services.load_Image_from_URL
import kotlinx.android.synthetic.main.fragment_user.*

/**
 * a user fragment, shows user daa
 */
class user_fragment : Fragment()
{
    /**
     * inflates the view
     * @param container the container of this fragment (activity view holder)
     * @param inflater the inflater in chrage of infalting this view
     * @param savedInstanceState the last state of this fragment
     * @return a view of this fragment
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.fragment_user, container, false)

    /**
     * when data loading is done
     */
    private var done:Boolean=false

    /**
     * is responisble of calling in a threadded way the function that loads and queries the data
     * required to show on this fragment, call is threadded and non-blocked
     * @param savedInstanceState the last state of this fragment
     * @exception IllegalStateException if not called from getInstance() and manually constructed instead and arguements weren't set
     */
    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        val main_progressbar = MainActivity.act.findViewById(R.id.main_progressbar) as ProgressBar
        main_progressbar.visibility = ProgressBar.VISIBLE
        Thread{
            /*
                subroutine to get user name and place it on fragment
             */
            val user_name = GitHub_remote_service.get_name()
            MainActivity.act.runOnUiThread {
                try
                {
                    user_greet.text = user_greet.text.toString().replace("name",user_name)
                }
                catch (e:NullPointerException)
                {
                    Log.d("User Fragment","No Username to replace")
                }
                catch (e:IllegalStateException)
                {
                    Log.d("User Fragment","No Username to replace")
                }
            }

            /*
                subroutine to download avy and set it on ImageView
             */
            try {
                load_Image_from_URL(GitHub_remote_service.get_pic_url(), main_avatar).execute()
            }
            catch (e:Exception)
            {
                Log.d("Avatar","Was null")
            }
            /*
                User repoistories list view initation
             */
            val user_repos = GitHub_remote_service.list_self_repositories()

            MainActivity.act.runOnUiThread {
                try
                {
                    repos_listview.adapter = array_adapter(context, user_repos)
                }
                catch (e:IllegalStateException)
                {
                    Log.d("User fragment","Weird kotlin error again...")
                }
            }

            /*
                user private info initation generate from web
             */
            val email = GitHub_remote_service.get_email()

            val location = GitHub_remote_service.get_location()

            val amount_of_repos = GitHub_remote_service.get_total_repos()

            val disk_usage = GitHub_remote_service.get_disk_usage()

            /*
                now, update the gained data into UI
             */
            MainActivity.act.runOnUiThread{

                try
                {
                    user_email.text = user_email.text.toString().replace("email",email)
                }
                catch (e:Exception)
                {
                    Log.d("User Fragment","No Email to replace")
                }

                try
                {
                    user_location.text = user_location.text.toString().replace("location",location)
                }
                catch (e:Exception)
                {
                    Log.d("User Fragment","No Location to replace")
                }

                try
                {
                    user_repos_count.text = user_repos_count.text.toString().replace("repos",amount_of_repos.toString())
                }
                catch (e:Exception)
                {
                    Log.d("User Fragment","No Repos count to replace")
                }

                try
                {
                    user_repos_count.text = user_repos_count.text.toString().replace("disk",disk_usage.toString())
                }
                catch (e:Exception)
                {
                    Log.d("User Fragment","No Disk usage to replace")
                }
                main_progressbar.visibility = ProgressBar.INVISIBLE
                done=true

            }
        }.start()

    }


    companion object
    {
        /**
         *  generator in a singleton-style of way, only this can be multi-instanced
         *  @return a instance of this fragment with that data sent
         */
        @Suppress("unused")
        fun newInstance(): user_fragment {
            return user_fragment()
        }
    }



}
