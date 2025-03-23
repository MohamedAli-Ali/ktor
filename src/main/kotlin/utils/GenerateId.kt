package com.froute.utils

import java.io.File
import java.util.concurrent.locks.ReentrantLock


object GenerateId {

    private  val idsDirectory = File("ids")
    private val voucherIdFile = File(idsDirectory,"last_voucher_id.txt")
    private val fruitIdFile = File(idsDirectory,"last_fruit_id.txt")
    private val userIdFile = File(idsDirectory,"last_user_id.txt")
    private val lock = ReentrantLock()

    init {
        if (!idsDirectory.exists()) {
            idsDirectory.mkdirs()
        }
    }

    fun getNextVoucherId(): Int {
        lock.lock()
        try {
            val currentId = if (voucherIdFile.exists()) {
                voucherIdFile.readText().toInt()
            } else {
                0
            }
            val nextId = currentId + 1
            voucherIdFile.writeText(nextId.toString())
            return nextId
        } finally {
            lock.unlock()
        }
    }

    fun getNextUserId(): Int {
        lock.lock()
        try {
            val currentId = if (userIdFile.exists()) {
                userIdFile.readText().toInt()
            } else {
                0
            }
            val nextId = currentId + 1
            userIdFile.writeText(nextId.toString())
            return nextId
        } finally {
            lock.unlock()
        }
    }


    fun getNextFruitId(): Int {
        lock.lock()
        try {
            val currentId = if (fruitIdFile.exists()) {
                fruitIdFile.readText().toInt()
            } else {
                0
            }
            val nextId = currentId + 1
            fruitIdFile.writeText(nextId.toString())
            return nextId
        } finally {
            lock.unlock()
        }
    }



    fun genVoucherCode(length: Int = 6): String {
        val allowedChars = ('A'..'Z')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }



}

