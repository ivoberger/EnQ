package com.ivoberger.enq.ui.viewmodel

import androidx.lifecycle.ViewModel;
import com.ivoberger.jmusicbot.JMusicBot

class UserInfoViewModel : ViewModel() {
    val user by lazy { JMusicBot.user }
}
