package diiin.ui.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.*
import br.com.gbmoro.diiin.R
import diiin.DindinApp
import diiin.dao.DataBaseFactory
import diiin.dao.LocalCacheManager
import diiin.model.Expense
import diiin.model.ExpenseType
import diiin.model.Salary
import diiin.ui.TWEditPrice
import diiin.util.MathService
import diiin.util.MessageDialog
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Define a contract between view and presenter.
 * MVP pattern.
 * @author Gabriel Moro
 * @since 24/08/2018
 * @version 1.0.9
 */
interface InsertExpenseContract {
    /**
     * Define all view operations.
     */
    interface View {
        fun showSucessMessage()
        fun showUnsucessMessage()
        fun setDescription(astrDescription: String)
        fun setDate(astrDate: String)
        fun setValue(astrValue: String)
        fun fillCategoriesInSpinnerContentAndSelectSomeone(alstValues: ArrayList<String>, anItem: Int)
    }

    /**
     * Define all operations connected to model layer
     */
    interface Presenter {
        fun loadExpenseValues(anExpenseId: Long?)
        fun loadCategories(anExpenseId: Long?)
        fun saveExpense(anExpenseId: Long?, astrCategory: String, astrDescription: String, astrValue: String, adtDate: Date)
    }
}

/**
 * Presenter of InsertExpenseScreen.
 * @author Gabriel Moro
 * @since 11/09/2018
 * @version 1.0.9
 */
class InsertExpensePresenter(avwView: InsertExpenseContract.View) : InsertExpenseContract.Presenter {

    private var view: InsertExpenseContract.View = avwView
    var dataBaseFactory: DataBaseFactory? = DindinApp.mlcmDataManager?.mappDataBaseBuilder

    /**
     * If user wants to update some expense, this function
     * is called to get the values of the expense that will be
     * updated.
     */
    override fun loadExpenseValues(anExpenseId: Long?) {
        anExpenseId ?: return
        DindinApp.mlcmDataManager?.getExpenseAccordingId(anExpenseId,
                object : LocalCacheManager.DatabaseCallBack {
                    override fun onSalaryObjectByIdReceived(aslSalary: Salary) {}
                    override fun onExpensesLoaded(alstExpenses: List<Expense>) {}
                    override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {}
                    override fun onSalariesLoaded(alstSalaries: List<Salary>) {}
                    override fun onExpenseIdReceived(aexpense: Expense) {
                        view.setDate(aexpense.mstrDate)
                        view.setDescription(aexpense.mstrDescription)
                        if (aexpense.msValue != null)
                            view.setValue(MathService.formatFloatToCurrency(aexpense.msValue!!))
                    }

                    override fun onExpenseTypeColorReceived(astrColor: String) {}
                    override fun onExpenseTypeDescriptionReceived(astrDescription: String) {}
                    override fun onExpenseTypeIDReceived(anID: Long?) {}
                })
    }

    /**
     * This method load all expense types available in app: food, pets.
     * @param anExpenseId is used to select the current expense type used
     * for expense (when update expense)
     */
    override fun loadCategories(anExpenseId: Long?) {

        val lstCategories = ArrayList<String>()

        if (anExpenseId != null) {
            DindinApp.mlcmDataManager?.getExpenseAccordingId(anExpenseId, object : LocalCacheManager.DatabaseCallBack {
                override fun onExpensesLoaded(alstExpenses: List<Expense>) {}
                override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {}
                override fun onSalariesLoaded(alstSalaries: List<Salary>) {}
                override fun onExpenseIdReceived(aexpense: Expense) {
                    val nExpenseTypeID = aexpense.mnExpenseType
                    DindinApp.mlcmDataManager?.getAllExpenseTypeObjects(object : LocalCacheManager.DatabaseCallBack {
                        override fun onExpensesLoaded(alstExpenses: List<Expense>) {}
                        override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {
                            var nCount = 0
                            alstExpensesType.forEachIndexed { nIndex, expense ->
                                lstCategories.add(expense.mstrDescription)
                                if (expense.mnExpenseTypeID == nExpenseTypeID) nCount = nIndex
                            }
                            view.fillCategoriesInSpinnerContentAndSelectSomeone(lstCategories, nCount)
                        }

                        override fun onSalariesLoaded(alstSalaries: List<Salary>) {}
                        override fun onExpenseIdReceived(aexpense: Expense) {}
                        override fun onExpenseTypeColorReceived(astrColor: String) {}
                        override fun onExpenseTypeDescriptionReceived(astrDescription: String) {}
                        override fun onExpenseTypeIDReceived(anID: Long?) {}
                        override fun onSalaryObjectByIdReceived(aslSalary: Salary) {}
                    })
                }

                override fun onExpenseTypeColorReceived(astrColor: String) {}
                override fun onExpenseTypeDescriptionReceived(astrDescription: String) {}
                override fun onExpenseTypeIDReceived(anID: Long?) {}
                override fun onSalaryObjectByIdReceived(aslSalary: Salary) {}
            })
        } else {
            DindinApp.mlcmDataManager?.getAllExpenseTypeObjects(object : LocalCacheManager.DatabaseCallBack {
                override fun onSalaryObjectByIdReceived(aslSalary: Salary) {}
                override fun onExpensesLoaded(alstExpenses: List<Expense>) {}
                override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {
                    alstExpensesType.forEach { expenseType -> lstCategories.add(expenseType.mstrDescription) }
                    view.fillCategoriesInSpinnerContentAndSelectSomeone(lstCategories, 0)
                }

                override fun onSalariesLoaded(alstSalaries: List<Salary>) {}
                override fun onExpenseIdReceived(aexpense: Expense) {}
                override fun onExpenseTypeColorReceived(astrColor: String) {}
                override fun onExpenseTypeDescriptionReceived(astrDescription: String) {}
                override fun onExpenseTypeIDReceived(anID: Long?) {}
            })
        }
    }

    /**
     * This function save the new expense or the expense update.
     * When expense update operation, the anExpenseId is not null.
     */
    override fun saveExpense(anExpenseId: Long?, astrCategory: String, astrDescription: String, astrValue: String, adtDate: Date) {
        if (astrValue.isEmpty()) {
            view.showUnsucessMessage()
        } else {
            val sValue = MathService.formatCurrencyValueToFloat(astrValue)
            val strDate = MathService.calendarTimeToString(adtDate, DindinApp.mstrDateFormat)

            DindinApp.mlcmDataManager?.getExpenseTypeID(astrCategory, object : LocalCacheManager.DatabaseCallBack {
                override fun onSalaryObjectByIdReceived(aslSalary: Salary) {}
                override fun onExpensesLoaded(alstExpenses: List<Expense>) {}
                override fun onExpenseTypeLoaded(alstExpensesType: List<ExpenseType>) {}
                override fun onSalariesLoaded(alstSalaries: List<Salary>) {}
                override fun onExpenseIdReceived(aexpense: Expense) {}
                override fun onExpenseTypeColorReceived(astrColor: String) {}
                override fun onExpenseTypeDescriptionReceived(astrDescription: String) {}
                override fun onExpenseTypeIDReceived(anID: Long?) {
                    val expenseTarget = Expense(anExpenseId, sValue, astrDescription, strDate, anID)
                    Observable.just(anExpenseId != null).subscribeOn(Schedulers.io()).subscribe {
                        if (it)
                            dataBaseFactory?.expenseDao()?.update(expenseTarget)
                        else
                            dataBaseFactory?.expenseDao()?.add(expenseTarget)
                    }
                    view.showSucessMessage()
                }
            })
        }
    }
}

/**
 * This screen is used by user to create the expense register
 *
 * @author Gabriel Moro
 */
class InsertExpenseActivity : AppCompatActivity(), InsertExpenseContract.View {

    companion object {
        const val INTENT_KEY_EXPENSEID: String = "IdOfExpenseToEdit"
    }

    private var presenter: InsertExpenseContract.Presenter? = null
    private var mspSpinnerExpenseType: Spinner? = null
    private var metValue: EditText? = null
    private var metDescription: EditText? = null
    private var mtvDate: TextView? = null
    private var mibChangeDate: ImageButton? = null
    private var clCalenderChoosed: Calendar = Calendar.getInstance()
    private var mbtSave: Button? = null
    private var mnExpenseId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_expense)

        mspSpinnerExpenseType = findViewById(R.id.spinnerExpenseType)
        metValue = findViewById(R.id.etPriceValue)
        metDescription = findViewById(R.id.etDescriptionValue)
        mtvDate = findViewById(R.id.tvDateChoosed)
        mibChangeDate = findViewById(R.id.ibChangeDate)
        mbtSave = findViewById(R.id.btnSave)

        if (intent.hasExtra(INTENT_KEY_EXPENSEID)) {
            mnExpenseId = intent.extras.getLong(INTENT_KEY_EXPENSEID)
            title = resources.getString(R.string.title_editexpense)
        }
        presenter = InsertExpensePresenter(this)
        presenter?.loadExpenseValues(mnExpenseId)
        presenter?.loadCategories(mnExpenseId)

    }

    override fun onStart() {
        super.onStart()

        mtvDate?.text = MathService.calendarTimeToString(clCalenderChoosed.time, DindinApp.mstrDateFormat)

        metValue?.addTextChangedListener(TWEditPrice(metValue!!))

        loadDataPickerListener()

        mbtSave?.setTextColor(ContextCompat.getColor(this, R.color.activityColorBackground))

        mbtSave?.setOnClickListener {
            val strValue = metValue?.text.toString()
            val strDescription = metDescription?.text.toString()
            val strExpenseType = mspSpinnerExpenseType?.selectedItem.toString()
            presenter?.saveExpense(mnExpenseId, strExpenseType, strDescription, strValue, clCalenderChoosed.time)
        }
    }

    /**
     * Define data picker setup.
     */
    private fun loadDataPickerListener() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            clCalenderChoosed.set(Calendar.YEAR, year)
            clCalenderChoosed.set(Calendar.MONTH, month)
            clCalenderChoosed.set(Calendar.DAY_OF_MONTH, day)

            if (MathService.isTheDateInCurrentYear(clCalenderChoosed.time)) {
                mtvDate?.text = MathService.calendarTimeToString(clCalenderChoosed.time, DindinApp.mstrDateFormat)
            } else {
                clCalenderChoosed = Calendar.getInstance()
                Toast.makeText(this, resources.getString(R.string.messageAboutWrongYear), Toast.LENGTH_LONG).show()
            }
            mtvDate?.text = MathService.calendarTimeToString(clCalenderChoosed.time, DindinApp.mstrDateFormat)
        }
        mibChangeDate?.setOnClickListener {
            DatePickerDialog(this, dateSetListener,
                    clCalenderChoosed.get(Calendar.YEAR),
                    clCalenderChoosed.get(Calendar.MONTH),
                    clCalenderChoosed.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    /**
     * Show the sucess message when user save a new expense or update it.
     */
    override fun showSucessMessage() {
        MessageDialog.showToastMessage(this, resources.getString(R.string.sucessaction))
    }

    /**
     * Show the unsucess message when user save a new expense or update it.
     */
    override fun showUnsucessMessage() {
        MessageDialog.showToastMessage(this, resources.getString(R.string.fillAreFields))
    }

    /**
     * Fill the spinner content with expense types available in app.
     */
    override fun fillCategoriesInSpinnerContentAndSelectSomeone(alstValues: ArrayList<String>, anItem: Int) {
        val lstArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, alstValues)
        lstArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        mspSpinnerExpenseType?.adapter = lstArrayAdapter
        mspSpinnerExpenseType?.setSelection(anItem)
    }

    /**
     * Change the description text value.
     */
    override fun setDescription(astrDescription: String) {
        metDescription?.setText(astrDescription)
    }

    /**
     * Change the date text value.
     */
    override fun setDate(astrDate: String) {
        mtvDate?.text = astrDate
    }

    /**
     * Change the value text.
     */
    override fun setValue(astrValue: String) {
        metValue?.setText(astrValue)
    }
}
