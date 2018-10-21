package diiin.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Button
import android.widget.EditText
import br.com.gbmoro.diiin.R
import diiin.DindinApp
import diiin.dao.LocalCacheManager
import diiin.model.Expense
import diiin.model.ExpenseType
import diiin.model.Salary
import diiin.ui.RVWithFLoatingButtonControl
import diiin.ui.adapter.ExpenseTypeListAdapter
import diiin.ui.adapter.ExpenseTypeListAdapterContract
import diiin.util.MessageDialog
import diiin.util.SelectionSharedPreferences
import java.util.*

/**
 * Define a contract between view and presenter.
 * MVP pattern.
 * @author Gabriel Moro
 * @since 24/08/2018
 * @version 1.0.9
 */
interface SettingsScreenContract {
    /**
     * Define all view operations.
     */
    interface View {
        fun callExpenseTypeInsertScreen()
        fun setExpenseTypeList(alstArrayList: ArrayList<ExpenseType>)
        fun setYear(astrYear: String)
    }

    /**
     * Define all operations connected to model layer
     */
    interface Presenter {
        fun saveYear(actxContext: Context, astrYear: String)
        fun loadYear()
        fun loadExpenseTypes()
    }
}

/**
 * Presenter of the SettingsScreen
 * @author Gabriel Moro
 * @since 11/09/2018
 * @version 1.0.9
 */
class SettingsPresenter(avwView: SettingsScreenContract.View) : SettingsScreenContract.Presenter {
    private val view: SettingsScreenContract.View = avwView

    /**
     * Save the year in shared preferences.
     */
    override fun saveYear(actxContext: Context, astrYear: String) {
        val nYear = astrYear.toIntOrNull() ?: return
        SelectionSharedPreferences.insertYearSelectPreference(actxContext, nYear)
    }

    /**
     * Load the last year defined for user.
     */
    override fun loadYear() {
        val strCurrentYear = DindinApp.mnYearSelected.toString()
        view.setYear(strCurrentYear)
    }

    /**
     * Load all expense types available in app.
     */
    override fun loadExpenseTypes() {
        DindinApp.mlcmDataManager?.getAllExpenseTypeObjects(object : LocalCacheManager.DatabaseCallBack {
            override fun onExpensesLoaded(alstExpenses: List<Expense>) {}
            override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {
                view.setExpenseTypeList(ArrayList(alstExpensesType))
                /**
                 * Update the global structure used to represent
                 * the expense types hashmap
                 */
                DindinApp.mhmExpenseType = HashMap()
                alstExpensesType.forEach { expense ->
                    if (expense.mnExpenseTypeID != null)
                        DindinApp.mhmExpenseType?.put(expense.mnExpenseTypeID!!, expense)
                }
            }

            override fun onSalariesLoaded(alstSalaries: List<Salary>) {}
            override fun onExpenseIdReceived(aexpense: Expense) {}
            override fun onExpenseTypeColorReceived(astrColor: String) {}
            override fun onExpenseTypeDescriptionReceived(astrDescription: String) {}
            override fun onExpenseTypeIDReceived(anID: Long?) {}
            override fun onSalaryObjectByIdReceived(aslSalary: Salary) {}
        })
    }

}

/**
 * View of the SettingsScreen.
 * @author Gabriel Moro
 * @since 11/09/2018
 * @version 1.0.9
 */
class SettingsActivity : AppCompatActivity(), SettingsScreenContract.View {

    private var mrvExpenseTypeList: RecyclerView? = null
    var mbtInsertExpenseType: FloatingActionButton? = null
    private var metYear: EditText? = null
    private var mbtnSaveYear: Button? = null
    private var presenter: SettingsScreenContract.Presenter? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        metYear = findViewById(R.id.etYearSelected)
        mbtnSaveYear = findViewById(R.id.btnToSaveTheYearSelected)
        mbtInsertExpenseType = findViewById(R.id.btnaddExpenseType)
        mrvExpenseTypeList = findViewById(R.id.rvExpenseTypeList)

        presenter = SettingsPresenter(this)

        val llManager = LinearLayoutManager(this)
        mrvExpenseTypeList?.layoutManager = llManager

        mbtInsertExpenseType?.setOnClickListener {
            callExpenseTypeInsertScreen()
        }
        mbtnSaveYear?.setOnClickListener {
            MessageDialog.showMessageDialog(this,
                    resources.getString(R.string.msgAreYouSure),
                    DialogInterface.OnClickListener { adialog, _ ->
                        val strYear = metYear?.text.toString()
                        presenter?.saveYear(this, strYear)
                    },
                    DialogInterface.OnClickListener { adialog, _ ->
                        adialog.dismiss()
                    })
        }
        if (mbtInsertExpenseType != null)
            mrvExpenseTypeList?.setOnTouchListener(RVWithFLoatingButtonControl(mbtInsertExpenseType!!))
    }

    override fun onResume() {
        super.onResume()
        presenter?.loadExpenseTypes()
        presenter?.loadYear()
    }

    /**
     * Call the screen to insert a new expense type
     */
    override fun callExpenseTypeInsertScreen() {
        startActivity(Intent(this, InsertExpenseTypeActivity::class.java))
    }

    /**
     * Update adapter list with the current expense types list.
     */
    override fun setExpenseTypeList(alstArrayList: ArrayList<ExpenseType>) {
        mrvExpenseTypeList?.adapter = ExpenseTypeListAdapter(object : ExpenseTypeListAdapterContract {
            override fun currentContext(): Context {
                return this@SettingsActivity
            }
        }, alstArrayList)
    }

    /**
     * Change the year text value.
     */
    override fun setYear(astrYear: String) {
        metYear?.setText(astrYear)
    }
}
