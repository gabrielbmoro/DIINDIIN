package diiin.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import br.com.gbmoro.diiin.R
import diiin.DindinApp
import diiin.dao.LocalCacheManager
import diiin.model.Expense
import diiin.model.ExpenseType
import diiin.model.MonthType
import diiin.model.Salary
import diiin.ui.RVWithFLoatingButtonControl
import diiin.ui.activity.InsertExpenseActivity
import diiin.ui.adapter.ExpenseListAdapter
import diiin.ui.adapter.ExpenseListAdapterContract
import diiin.ui.adapter.RefreshData
import diiin.util.MathService
import java.util.*
import kotlin.collections.ArrayList

/**
 * Define a contract between view and presenter.
 * MVP pattern.
 * @author Gabriel Moro
 * @since 24/08/2018
 * @version 1.0.9
 */
interface ExpenseListContract {
    /**
     * Define all view operations.
     */
    interface View {
        fun loadExpenseListAdapter(alstExpenses: ArrayList<Expense>)
    }

    /**
     * Define all operations connected to model layer
     */
    interface Presenter {
        fun loadExpenses(amnMonthSelected: MonthType, anYearSelected: Int)
    }
}

/**
 * Define the presenter to expense list fragment.
 * @author Gabriel Moro
 * @since 11/09/2018
 * @version 1.0.9
 */
class FragmentExpenseListPresenter(avwView: ExpenseListContract.View) : ExpenseListContract.Presenter {

    private val view: ExpenseListContract.View = avwView

    /**
     * Load all expenses according to month and year selected for user.
     */
    override fun loadExpenses(amnMonthSelected: MonthType, anYearSelected: Int) {
        DindinApp.mlcmDataManager?.getAllExpenses(object : LocalCacheManager.DatabaseCallBack {
            override fun onExpensesLoaded(alstExpenses: List<Expense>) {

                val lstFilteredList: ArrayList<Expense> = ArrayList()

                alstExpenses.forEach { expense ->
                    val clCalendar = Calendar.getInstance()
                    clCalendar.time = MathService.stringToCalendarTime(expense.mstrDate, DindinApp.mstrDateFormat)
                    if (clCalendar.get(Calendar.MONTH) == amnMonthSelected.aid && clCalendar.get(Calendar.YEAR) == anYearSelected) {
                        lstFilteredList.add(expense)
                    }
                }
                view.loadExpenseListAdapter(lstFilteredList)
            }

            override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {}
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
 * Screen that shows to user the expenses filter and list
 *
 * @author Gabriel Moro
 * @since 11/09/2018
 * @version 1.0.9
 */
class FragmentExpensesList : Fragment(), RefreshData, ExpenseListContract.View {

    private var mspMonthSelector: Spinner? = null
    private var mrvExpenseList: RecyclerView? = null
    var mbtInsertExpense: FloatingActionButton? = null
    private var presenter: ExpenseListContract.Presenter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_expenseslist, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mspMonthSelector = view?.findViewById(R.id.spMonthSelector)
        mrvExpenseList = view?.findViewById(R.id.rvExpenseList)
        mbtInsertExpense = view?.findViewById(R.id.btnaddExpense)
        mbtInsertExpense?.setOnClickListener {
            activity.startActivity(Intent(activity, InsertExpenseActivity::class.java))
        }

        val llManager = LinearLayoutManager(context)
        mrvExpenseList?.layoutManager = llManager
        loadExpenseListAdapter(ArrayList())
        if (mbtInsertExpense != null)
            mrvExpenseList?.setOnTouchListener(RVWithFLoatingButtonControl(mbtInsertExpense!!))

        presenter = FragmentExpenseListPresenter(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /**
     * Refresh is used to restart the adapter list content with the current state.
     */
    override fun refresh() {
        val mnMonthSelected = DindinApp.mmtMonthSelected ?: return
        val mnYearSelected = DindinApp.mnYearSelected ?: return
        presenter?.loadExpenses(mnMonthSelected, mnYearSelected)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        mrvExpenseList?.adapter?.notifyDataSetChanged()
    }

    /**
     * Change the adapter list.
     */
    override fun loadExpenseListAdapter(alstExpenses: ArrayList<Expense>) {
        val elAdapter = ExpenseListAdapter(
                object : ExpenseListAdapterContract {
                    override fun onRemoveItem() =
                            (activity.application as DindinApp)
                                    .bus()
                                    .send(true)

                    override fun currentContext(): Context = context
                }, alstExpenses)
        mrvExpenseList?.adapter = elAdapter
    }


}