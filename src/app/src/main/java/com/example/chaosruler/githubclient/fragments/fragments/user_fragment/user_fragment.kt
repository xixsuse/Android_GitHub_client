package com.example.chaosruler.githubclient.fragments.fragments.user_fragment


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.chaosruler.githubclient.R
import com.example.chaosruler.githubclient.activities.MainActivity
import com.example.chaosruler.githubclient.services.GitHub_remote_service
import kotlinx.android.synthetic.main.fragment_user.*
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL


class user_fragment : Fragment()
{

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.fragment_user, container, false)

    private var done:Boolean=false
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
            }

            /*
                subroutine to download avy and set it on ImageView
             */
            load_Image_from_URL(GitHub_remote_service.get_pic_url(),main_avatar).execute()

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

    /*
        generator
     */
    companion object
    {

        @Suppress("unused")
        fun newInstance(): user_fragment {
            return user_fragment()
        }
    }


    /*
        class responsible for downloading image and setting avy as that image
     */
    @SuppressLint("StaticFieldLeak")
    inner class load_Image_from_URL(private val url:String, private var imageView: ImageView): AsyncTask<String, Void, Bitmap?>()
    {

        override fun doInBackground(vararg params: String?): Bitmap?
        {
            try
            {
                /*
                    builds a URL
                 */
                val avatar_URL = URL(url)
                /*
                     opens a connection
                 */
                val connection_to_download_avy = avatar_URL.openConnection()
                connection_to_download_avy.doInput = true
                connection_to_download_avy.connect()
                /*
                    get input stream from data
                 */
                val input_stream: InputStream = connection_to_download_avy.getInputStream()
                /*
                    decode input stream into a image
                 */
                return BitmapFactory.decodeStream(input_stream)
            }
            catch (e:MalformedURLException)
            {
                /*
                    url is incorrect
                 */
                Log.d("User Fragment","malformed URL")
            }
            catch (e:Exception)
            {
                /*
                anything wrong happened
                 */
                Log.d("User Fragment","Something went wrong")
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            /*
                upon success
             */
            if(result!=null)
            {
                /*
                update image on UI if download was successfull and decode was sucessfull
                 */
                MainActivity.act.runOnUiThread { imageView.setImageBitmap(result) }
            }
        }

    }
}