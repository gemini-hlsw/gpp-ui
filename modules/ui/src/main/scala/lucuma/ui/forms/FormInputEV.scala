// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ui.forms

import scala.scalajs.js
import scala.scalajs.js.|

import cats.syntax.all._
import cats.Eq
import japgolly.scalajs.react._
import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react.raw.JsNumber
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import lucuma.ui.optics.ValidFormatInput
import react.common._
import react.semanticui._
import react.semanticui.collections.form.FormInput
import react.semanticui.elements.icon.Icon
import react.semanticui.elements.input._
import react.semanticui.elements.label._
import cats.data.NonEmptyChain
import scalajs.js.JSConverters._
import cats.data.Validated.Valid
import cats.data.Validated.Invalid
import cats.data.ValidatedNec
import eu.timepit.refined.types.string.NonEmptyString

/**
 * FormInput component that uses an ExternalValue to share the content of the field
 */
final case class FormInputEV[EV[_], A](
  id:              String,
  action:          js.UndefOr[ShorthandSB[VdomNode]] = js.undefined,
  actionPosition:  js.UndefOr[ActionPosition] = js.undefined,
  as:              js.UndefOr[AsC] = js.undefined,
  className:       js.UndefOr[String] = js.undefined,
  clazz:           js.UndefOr[Css] = js.undefined,
  content:         js.UndefOr[ShorthandS[VdomNode]] = js.undefined,
  control:         js.UndefOr[String] = js.undefined,
  disabled:        js.UndefOr[Boolean] = js.undefined,
  error:           js.UndefOr[ShorthandB[NonEmptyString]] = js.undefined,
  errorClazz:      js.UndefOr[Css] = js.undefined,
  errorPointing:   js.UndefOr[LabelPointing] = js.undefined,
  fluid:           js.UndefOr[Boolean] = js.undefined,
  focus:           js.UndefOr[Boolean] = js.undefined,
  icon:            js.UndefOr[ShorthandSB[Icon]] = js.undefined,
  iconPosition:    js.UndefOr[IconPosition] = js.undefined,
  inline:          js.UndefOr[Boolean] = js.undefined,
  input:           js.UndefOr[VdomNode] = js.undefined,
  inverted:        js.UndefOr[Boolean] = js.undefined,
  label:           js.UndefOr[ShorthandS[Label]] = js.undefined,
  labelPosition:   js.UndefOr[LabelPosition] = js.undefined,
  loading:         js.UndefOr[Boolean] = js.undefined,
  required:        js.UndefOr[Boolean] = js.undefined,
  size:            js.UndefOr[SemanticSize] = js.undefined,
  tabIndex:        js.UndefOr[String | JsNumber] = js.undefined,
  tpe:             js.UndefOr[String] = js.undefined,
  transparent:     js.UndefOr[Boolean] = js.undefined,
  width:           js.UndefOr[SemanticWidth] = js.undefined,
  value:           EV[A],
  validFormat:     ValidFormatInput[A] = ValidFormatInput.id,
  modifiers:       Seq[TagMod] = Seq.empty,
  onTextChange:    String => Callback = _ => Callback.empty,
  onValidChange:   FormInputEV.ChangeCallback[Boolean] = _ => Callback.empty,
  onBlur:          FormInputEV.ChangeCallback[ValidatedNec[NonEmptyString, A]] =
    // Only use for extra actions, setting should be done through value.set
    (_: ValidatedNec[NonEmptyString, A]) => Callback.empty
)(implicit val ev: ExternalValue[EV], val eq: Eq[A])
    extends ReactProps[FormInputEV[Any, Any]](FormInputEV.component) {

  def valGet: String = ev.get(value).foldMap(validFormat.reverseGet)

  def valSet: InputEV.ChangeCallback[A] = ev.set(value)

  def withMods(mods: TagMod*): FormInputEV[EV, A] = copy(modifiers = modifiers ++ mods)
}

object FormInputEV {
  type Props[EV[_], A]   = FormInputEV[EV, A]
  type ChangeCallback[A] = A => Callback

  @Lenses
  final case class State(
    curValue:  String,
    prevValue: String,
    errors:    Option[NonEmptyChain[NonEmptyString]]
  )

  class Backend[EV[_], A]($ : BackendScope[Props[EV, A], State]) {

    def validate(
      props: Props[EV, A],
      value: String,
      cb:    ValidatedNec[NonEmptyString, A] => Callback = _ => Callback.empty
    ): Callback = {
      val validated = props.validFormat.getValidated(value)
      props.onValidChange(validated.isValid) >> cb(validated)
    }

    def onTextChange(props: Props[EV, A]): ReactEventFromInput => Callback =
      (e: ReactEventFromInput) => {
        // Capture the value outside setState, react reuses the events
        val v = e.target.value
        // First update the internal state, then call the outside listener
        $.setStateL(State.curValue)(v) *> $.setStateL(State.errors)(none) *>
          props.onTextChange(v) *>
          validate(props, v)
      }

    def onBlur(props: Props[EV, A], state: State): Callback =
      validate(
        props,
        state.curValue,
        { validated =>
          val validatedCB = validated match {
            case Valid(a)   =>
              implicit val eq = props.eq
              if (props.ev.get(props.value).exists(_ =!= a)) // Only set if resulting A changed.
                props.valSet(a)
              else                                           // A didn't change, but redisplay formatted string.
                $.setStateL(State.curValue)(props.valGet)
            case Invalid(e) =>
              $.setStateL(State.errors)(e.some)
          }
          validatedCB >> props.onBlur(validated)
        }
      )

    def render(p: Props[EV, A], s: State): VdomNode = {

      def errorLabel(errors: NonEmptyChain[NonEmptyString]): js.UndefOr[ShorthandB[Label]] = {
        val vdoms = errors.toList.map[VdomNode](_.value)
        val list  = vdoms.head +: vdoms.tail.flatMap[VdomNode](e => List(<.br, <.br, e))
        Label(
          content = React.Fragment(list: _*),
          clazz = p.errorClazz,
          pointing = p.errorPointing
        )(
          ^.position.absolute
        )
      }

      val error: js.UndefOr[ShorthandB[Label]] = p.error
        .flatMap[ShorthandB[Label]] {
          (_: Any) match {
            case b: Boolean => s.errors.map(errorLabel).getOrElse(b)
            case e          => // We can't pattern match against NonEmptyString, but we know it is one.
              val nes = e.asInstanceOf[NonEmptyString]
              s.errors.map(ve => errorLabel(nes +: ve)).getOrElse(errorLabel(NonEmptyChain(nes)))
          }
        }
        .orElse(s.errors.orUndefined.flatMap(errorLabel))

      FormInput(
        p.action,
        p.actionPosition,
        p.as,
        p.className,
        p.clazz,
        p.content,
        p.control,
        p.disabled,
        error,
        p.fluid,
        p.focus,
        p.icon,
        p.iconPosition,
        p.inline,
        p.input,
        p.inverted,
        p.label,
        p.labelPosition,
        p.loading,
        js.undefined,
        onTextChange(p),
        p.required,
        p.size,
        p.tabIndex,
        p.tpe,
        p.transparent,
        p.width,
        s.curValue
      )(
        (p.modifiers :+ (^.id := p.id) :+ (^.onBlur --> onBlur(p, s)): _*)
      )
    }
  }

  protected def buildComponent[EV[_], A] =
    ScalaComponent
      .builder[Props[EV, A]]
      .getDerivedStateFromPropsAndState[State] { (props, stateOpt) =>
        val newValue = props.valGet
        // Force new value from props if the prop changes (or we are initializing).
        stateOpt match {
          case Some(state) if newValue === state.prevValue => state
          case _                                           => State(newValue, newValue, none)
        }
      }
      .renderBackend[Backend[EV, A]]
      .componentDidMount($ => $.backend.validate($.props, $.props.valGet))
      .build

  protected val component = buildComponent[Any, Any]
}
