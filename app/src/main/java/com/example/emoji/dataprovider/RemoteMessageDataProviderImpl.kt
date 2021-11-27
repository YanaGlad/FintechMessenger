package com.example.emoji.dataprovider

import com.example.emoji.api.Api
import com.example.emoji.api.model.*
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class RemoteMessageDataProviderImpl @Inject constructor(
    private val api: Api,
): RemoteMessageDataProvider {

    override fun getMessages(queryMap: Map<String, String>): Single<List<Message>> {
        return api.getMessages(queryMap).flatMap { response ->
            Single.just(response.oneMessages)
        }
            .map { it ->
                it.map {
                    Message(
                        id = it.id,
                        topicName = it.topicName,
                        authorId = it.authorId,
                        authorName = it.authorName,
                        avatarUrl = it.avatarUrl,
                        content = it.content,
                        time = it.time,
                        reactions = it.reactions,
                        isMeMessage = it.isMeMessage
                    )
                }
            }
    }

    override fun sendMessage(queryMap: Map<String, String>): Completable {
        return api.sendMessage(queryMap)
    }

    override fun addMessageReaction(id: Int, reactionName: String): Single<Reaction> {
        return api.addMessageReaction(id, reactionName)
    }

    override fun removeMessageReaction(id: Int, reactionName: String): Completable {
        return api.removeMessageReaction(id, reactionName)
    }
}