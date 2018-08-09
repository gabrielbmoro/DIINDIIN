package diiin.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import br.com.gbmoro.diiin.R
import diiin.StaticCollections
import diiin.model.ExpenseType
import diiin.util.MathService
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import diiin.model.Expense
import diiin.ui.activity.MainActivity
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap

/**
 * Screen that shows to user the financial overview
 *
 * @author Gabriel Moro
 */
class FragmentFinancialReport : Fragment(), MainActivity.MainPageFragments {


    companion object {
        const val NAME = "FragmentFinancialReport"
    }

    private var mpcPieChart: PieChart? = null
    private var mrlChartItem : RelativeLayout? = null
    private var mrlPieChartContainer : RelativeLayout? = null
    private var mrlWalletPanel : RelativeLayout? = null
    private var mtvExpenseTotalValue : TextView? = null
    private var mtvSalaryTotalValue : TextView? = null
    private var mtvWalletTotalValue : TextView? = null
    private val mhmExpenseByPercentage : HashMap<ExpenseType, Expense> = HashMap()
    /**
     * Chart item elements
     */
    private var mtvChartItemValue : TextView? = null
    private var mtvChartItemDate : TextView? = null
    private var mvwChartItemExpenseType : View? = null
    private var mllChartItemLinearLayout : LinearLayout? = null
    private var mivChartItemReorder : ImageView? = null
    private var mivExpenseType : ImageView? = null
    private var mtvChartItemExpenseType : TextView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_financialreport, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        mpcPieChart = view?.findViewById(R.id.pchart)
        mrlPieChartContainer = view?.findViewById(R.id.rlPieChart)
        mrlWalletPanel = view?.findViewById(R.id.rlWalletPanel)
        mtvExpenseTotalValue = view?.findViewById(R.id.tvExpenseTotalValue)
        mtvSalaryTotalValue = view?.findViewById(R.id.tvSalaryValue)
        mtvWalletTotalValue = view?.findViewById(R.id.tvWalletValue)
        mrlChartItem = view?.findViewById(R.id.rlChartItem)
        mtvChartItemValue = view?.findViewById(R.id.tvValue)
        mtvChartItemDate = view?.findViewById(R.id.tvDate)
        mtvChartItemExpenseType = view?.findViewById(R.id.tvExpenseType)
        mvwChartItemExpenseType = view?.findViewById(R.id.vwExpenseType)
        mllChartItemLinearLayout = view?.findViewById(R.id.llLine2)
        mivChartItemReorder = view?.findViewById(R.id.ivReorder)
        mivExpenseType = view?.findViewById(R.id.ivExpenseType)

        mpcPieChart?.setUsePercentValues(true)
        mpcPieChart?.description?.isEnabled = false
        mpcPieChart?.setExtraOffsets(2f, 5f, 2f, 2f)
        mpcPieChart?.dragDecelerationFrictionCoef = 0.95f
        mpcPieChart?.rotationAngle = 0f
        mpcPieChart?.isRotationEnabled = false
        mpcPieChart?.isDrawHoleEnabled = true
        mpcPieChart?.transparentCircleRadius = 10f
        mpcPieChart?.holeRadius = 7f
        mpcPieChart?.setHoleColor(ContextCompat.getColor(context,R.color.whiteColor))

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        mrlChartItem?.visibility = RelativeLayout.GONE
        mrlWalletPanel?.visibility = RelativeLayout.VISIBLE
        loadPageContent()
    }

    private fun initPieEntry(asFloatPercent : Float, astrString : String) : PieEntry {
        val entry = PieEntry(asFloatPercent)
        entry.label = astrString
        return entry
    }

    override fun loadPageContent() {
        mhmExpenseByPercentage.clear()

        var sTotalExpenseMonth = 0f
        val lstExpensesOfMonth = ArrayList<Expense>()
        val lstEntries = ArrayList<PieEntry>()
        val lstColors = ArrayList<Int>()

        StaticCollections.mappDataBuilder?.expenseDao()?.all()?.forEach {
            val clCalendar = Calendar.getInstance()
            clCalendar.time = MathService.stringToCalendarTime(it.mstrDate, StaticCollections.mstrDateFormat)
            if(clCalendar.get(Calendar.MONTH)== StaticCollections.mmtMonthSelected?.aid) {
                lstExpensesOfMonth.add(it)
                if(it.msValue != null) sTotalExpenseMonth+=it.msValue!!
            }
        }


        lstExpensesOfMonth.sortWith(
                Comparator { t0: Expense, t1: Expense ->
                    if(t0.mnExpenseType != null && t1.mnExpenseType != null) t0.mnExpenseType!!.compareTo(t1.mnExpenseType!!)
                    else {
                        if(t0.mnExpenseType == null) -1
                        else if(t1.mnExpenseType == null) 1
                        else 0
                    }
                }
        )

        val iterator = lstExpensesOfMonth.iterator()
        var currentCategory : Long? = null
        var sValueCurrentCategory = 0f
        while (iterator.hasNext()) {
            val expenseTarget = iterator.next()
            if (currentCategory == null)
                currentCategory = expenseTarget.mnExpenseType

            if (expenseTarget.mnExpenseType == currentCategory) {
                if (expenseTarget.msValue != null)
                    sValueCurrentCategory += expenseTarget.msValue!!
            } else {
                val strDescription = StaticCollections.mappDataBuilder?.expenseTypeDao()?.getDescription(currentCategory!!)
                if(strDescription != null)
                {
                    lstEntries.add(initPieEntry(sValueCurrentCategory, strDescription))
                    lstColors.add(ContextCompat.getColor(context, R.color.petsbkg))
                }
                sValueCurrentCategory = 0f
                currentCategory = null
            }
        }


        val dataSet = PieDataSet(lstEntries, "")
        dataSet.sliceSpace = 2f
        dataSet.formLineWidth = 120f
        dataSet.colors = lstColors
        val dataPie = PieData(dataSet)
        dataPie.setValueFormatter(PercentFormatter())
        dataPie.setValueTextSize(14f)
        dataPie.setValueTextColor(Color.WHITE)
        mpcPieChart?.legend?.textColor = ContextCompat.getColor(context, R.color.whiteColor)
        mpcPieChart?.legend?.textSize = 12f
        mpcPieChart?.legend?.isWordWrapEnabled = true
        mpcPieChart?.setDrawEntryLabels(false)
        mpcPieChart?.data = dataPie
        mpcPieChart?.highlightValues(null)
        mpcPieChart?.invalidate()
//
        mpcPieChart?.setOnChartValueSelectedListener(object : OnChartValueSelectedListener{
            override fun onNothingSelected() {
                mrlChartItem?.visibility = RelativeLayout.GONE
                mrlWalletPanel?.visibility = RelativeLayout.VISIBLE
            }

            override fun onValueSelected(e: Entry, h: Highlight?) {

//                val pieEntry = e as PieEntry
//                val nId = ExpenseType.gettingIdFromDescription(context, pieEntry.label) ?: return
//                val expenseTarget = mhmExpenseByPercentage[ExpenseType.fromInt(nId)]
//                if(expenseTarget != null)
//                    loadChartItemCard(expenseTarget)
            }
        })

        mtvExpenseTotalValue?.text = MathService.formatFloatToCurrency(sTotalExpenseMonth)

//        var sTotalSalary = 0f
//        StaticCollections.mastSalary?.forEach {
//            val clCalendar = Calendar.getInstance()
//            clCalendar.time = it.mdtDate
//            if(clCalendar.get(Calendar.MONTH) == StaticCollections.mmtMonthSelected?.aid && clCalendar.get(Calendar.YEAR) == StaticCollections.mnYearSelected){
//                if(it.msValue != null)
//                    sTotalSalary += it.msValue!!
//            }
//        }
//
//        mtvSalaryTotalValue?.text = MathService.formatFloatToCurrency(sTotalSalary)
//
//        val sWalletValue = sTotalSalary - sSumTotal
//        mtvWalletTotalValue?.text = MathService.formatFloatToCurrency(sWalletValue)
//
//        mrlChartItem?.visibility = RelativeLayout.GONE
    }



    private fun loadChartItemCard(aExpenseItem : Expense?) {
        aExpenseItem ?: return

        mivChartItemReorder?.visibility = ImageView.GONE
        mtvChartItemDate?.visibility = TextView.GONE

        if(aExpenseItem.msValue != null) mtvChartItemValue?.text = MathService.formatFloatToCurrency(aExpenseItem.msValue!!)

//        if(aExpenseItem.metType != null) {
//            mtvChartItemExpenseType?.text = aExpenseItem.metType.description(context)
//            mtvChartItemValue?.setTextColor(aExpenseItem.metType.backgroundColor(context))
//            mvwChartItemExpenseType?.setBackgroundColor(aExpenseItem.metType.backgroundColor(context))
//            mivChartItemReorder?.visibility = ImageView.GONE
//            mivExpenseType?.setImageResource(aExpenseItem.metType.imageIconId())
//        }

        mrlChartItem?.visibility = RelativeLayout.VISIBLE
        mrlWalletPanel?.visibility = RelativeLayout.GONE
    }

}