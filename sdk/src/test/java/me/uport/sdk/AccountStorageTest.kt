package me.uport.sdk

import assertk.all
import assertk.assert
import assertk.assertions.*
import me.uport.sdk.fakes.InMemorySharedPrefs
import me.uport.sdk.identity.Account
import org.junit.Test

class AccountStorageTest {

    @Test
    fun `can add and retrieve new account`() {
        val storage: AccountStorage = SharedPrefsAccountStorage(InMemorySharedPrefs())
        val newAcc = Account("0xnewaccount", "", "", "", "", "", "")
        storage.upsert(newAcc)
        assert(storage.get("0xnewaccount")).isEqualTo(newAcc)
    }

    @Test
    fun `can show all accounts`() {
        val storage: AccountStorage = SharedPrefsAccountStorage(InMemorySharedPrefs())

        val accounts = (0..10).map {
            Account("0x$it", "", "", "", "", "", "")
        }.map {
            storage.upsert(it)
            it
        }

        val allAccounts = storage.all()

        assert(allAccounts.containsAll(accounts))
    }

    @Test
    fun `can delete account`() {
        val storage: AccountStorage = SharedPrefsAccountStorage(InMemorySharedPrefs())

        val refAccount = Account(
                "0xmyAccount",
                "device",
                "0x1",
                "0xpublic",
                "",
                "",
                ""
        )

        storage.upsert(refAccount)
        storage.upsert(refAccount)
        assert(storage.get(refAccount.handle)).isEqualTo(refAccount)

        storage.delete(refAccount.handle)

        assert(storage.get(refAccount.handle)).isNull()
        assert(storage.all()).doesNotContain(refAccount)
    }

    @Test
    fun `can overwrite account`() {
        val storage: AccountStorage = SharedPrefsAccountStorage(InMemorySharedPrefs())

        val refAccount = Account(
                "0xmyAccount",
                "device",
                "0x1",
                "0xpublic",
                "",
                "",
                ""
        )

        storage.upsert(refAccount)

        val newAccount = refAccount.copy(isDefault = true)

        storage.upsert(newAccount)

        assert(storage.get(refAccount.handle)).all {
            isNotEqualTo(refAccount)
            isEqualTo(newAccount)
        }
        assert(storage.all()).all {
            doesNotContain(refAccount)
            contains(newAccount)
        }
    }

    @Test
    fun `can upsert all`() {
        val storage: AccountStorage = SharedPrefsAccountStorage(InMemorySharedPrefs())

        val accounts = (0..10).map {
            Account("0x$it", "", "", "", "", "", "")
        }

        storage.upsertAll(accounts)

        val allAccounts = storage.all()

        assert(allAccounts.containsAll(accounts))
    }

}