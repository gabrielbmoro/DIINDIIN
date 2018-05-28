package com.example.gabrielbronzattimoro.diiin.ui.activity

import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.gabrielbronzattimoro.diiin.R
import com.example.gabrielbronzattimoro.diiin.util.SelectionSharedPreferences
import com.example.gabrielbronzattimoro.diiin.model.MonthType
import com.example.gabrielbronzattimoro.diiin.StaticCollections
import com.example.gabrielbronzattimoro.diiin.ui.ActivityDeleteCellsFromList
import com.example.gabrielbronzattimoro.diiin.ui.fragments.FragmentExpensesList
import com.example.gabrielbronzattimoro.diiin.ui.fragments.FragmentFinancialReport
import com.example.gabrielbronzattimoro.diiin.ui.fragments.FragmentSalaryList
import com.example.gabrielbronzattimoro.diiin.util.MessageDialog
import com.example.gabrielbronzattimoro.diiin.util.SharedPreferenceConnection


class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, ActivityDeleteCellsFromList {

    private var mnvNavigation : BottomNavigationView? = null
    private var mstCurrentFragment : String? = null
    private var mfgCurrentFragment : Fragment? = null
    private var mspMonthSelector : Spinner? = null
    private var mltSpinnerMonthsList : ArrayList<String>? = null
    private var mMenuInflated : Menu? = null
    private var mnCountToVisibleRemove : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mnvNavigation = findViewById(R.id.bnvNavigation)
        mspMonthSelector = findViewById(R.id.spMonthSelector)

        mnvNavigation?.setOnNavigationItemSelectedListener(this)

        loadSpinnersContent()
    }

    override fun onResume() {
        super.onResume()

        loadFragments()
    }

    private fun loadFragments() {
        if (mstCurrentFragment == null)
            mstCurrentFragment = FragmentExpensesList.NAME

        if (mfgCurrentFragment != null)
            supportFragmentManager.beginTransaction().remove(mfgCurrentFragment).commit()

        mfgCurrentFragment = when (mstCurrentFragment) {
            FragmentSalaryList.NAME -> FragmentSalaryList()
            FragmentFinancialReport.NAME -> FragmentFinancialReport()
            FragmentExpensesList.NAME -> FragmentExpensesList()
            else -> null
        }
        if (mfgCurrentFragment != null)
            supportFragmentManager.beginTransaction().replace(R.id.flMainContent, mfgCurrentFragment).commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_piechart -> {
                if(mstCurrentFragment != FragmentFinancialReport.NAME) {
                    resetRemoveMenu()
                    supportFragmentManager.beginTransaction().remove(mfgCurrentFragment).commit()
                    mfgCurrentFragment = FragmentFinancialReport()
                    supportFragmentManager.beginTransaction().replace(R.id.flMainContent, mfgCurrentFragment).commit()
                    mstCurrentFragment = FragmentFinancialReport.NAME
                }
                return true
            }
            R.id.navigation_expenses -> {
                if(mstCurrentFragment != FragmentExpensesList.NAME) {
                    resetRemoveMenu()
                    supportFragmentManager.beginTransaction().remove(mfgCurrentFragment).commit()
                    mfgCurrentFragment = FragmentExpensesList()
                    supportFragmentManager.beginTransaction().replace(R.id.flMainContent, mfgCurrentFragment).commit()
                    mstCurrentFragment = FragmentExpensesList.NAME
                }
                return true
            }
            R.id.navigation_salary -> {
                if(mstCurrentFragment != FragmentSalaryList.NAME) {
                    resetRemoveMenu()
                    supportFragmentManager.beginTransaction().remove(mfgCurrentFragment).commit()
                    mfgCurrentFragment = FragmentSalaryList()
                    supportFragmentManager.beginTransaction().replace(R.id.flMainContent, mfgCurrentFragment).commit()
                    mstCurrentFragment = FragmentSalaryList.NAME
                }
                return true
            }
        }
        return false
    }

    private fun loadSpinnersContent() {
        mltSpinnerMonthsList = ArrayList()
        mltSpinnerMonthsList?.add(resources.getString(R.string.All))

        MonthType.values().forEach { mltSpinnerMonthsList?.add(it.description(this)) }

        val lstArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mltSpinnerMonthsList)
        lstArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mspMonthSelector?.adapter = lstArrayAdapter
        mspMonthSelector?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val idOfMonth = MonthType.gettingIdFromDescription(baseContext, mspMonthSelector?.selectedItem.toString())
                StaticCollections.mmtMonthSelected = if(idOfMonth != null) {
                    val monthType = MonthType.fromInt(idOfMonth)
                    monthType
                } else { null } ?: return

                SelectionSharedPreferences.insertMonthSelectPreference(baseContext, StaticCollections.mmtMonthSelected)
                mstCurrentFragment ?: return

                when(mstCurrentFragment) {
                    FragmentSalaryList.NAME -> {
                        (mfgCurrentFragment as FragmentSalaryList).loadSalaryList()
                    }
                    FragmentExpensesList.NAME -> {
                        (mfgCurrentFragment as FragmentExpensesList).loadExpenseList()
                    }
                    FragmentFinancialReport.NAME -> {
                        (mfgCurrentFragment as FragmentFinancialReport).loadChartData()
                    }
                }
            }
        }

        mltSpinnerMonthsList ?: return
        StaticCollections.mmtMonthSelected ?: return

        val strMonthValue = StaticCollections.mmtMonthSelected?.description(this)


        var nCount = 0
        val nSize = mltSpinnerMonthsList?.size
        while(nCount < nSize!!) {
            if(mltSpinnerMonthsList!![nCount]==strMonthValue)
                break
            nCount++
        }
        mspMonthSelector?.setSelection(nCount)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater?.inflate(R.menu.useroptions, menu)
        menuInflater?.inflate(R.menu.deleteoption, menu)
        mMenuInflated = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return false

        when(item.itemId) {
            R.id.menu_clearalldata -> {
                MessageDialog.showMessageDialog(this,
                        resources.getString(R.string.msgAreYouSure),
                        DialogInterface.OnClickListener { adialog, _ ->
                            SharedPreferenceConnection.clearAllPreferences(this)
                            MessageDialog.showToastMessage(this, resources.getString(R.string.pleaseRestartTheApp))
                            adialog.dismiss()
                        },
                        DialogInterface.OnClickListener { adialog, _ ->
                            adialog.dismiss()
                        })
            }
            R.id.remove -> {
                resetRemoveMenu()
            }
            else -> { }
        }
        return true
    }

    private fun resetRemoveMenu() {
        mnCountToVisibleRemove = 0
        mMenuInflated?.findItem(R.id.remove)?.isVisible = false
    }

    override fun hideMenu() {
        if(mnCountToVisibleRemove > 0) mnCountToVisibleRemove--

        if(mnCountToVisibleRemove==0)
            mMenuInflated?.findItem(R.id.remove)?.isVisible = false
    }

    override fun showMenu() {
        mnCountToVisibleRemove++
        mMenuInflated?.findItem(R.id.remove)?.isVisible = true
    }

}

