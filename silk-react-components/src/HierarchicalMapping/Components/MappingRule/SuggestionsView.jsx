import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Spinner,
    Error,
    Checkbox,
    Button,
    AffirmativeButton,
    DismissiveButton,
    ContextMenu,
    MenuItem,
    Chip,
} from 'ecc-gui-elements';
import SuggestionView from './SuggestionView';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';


const SuggestionsView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        targets: React.PropTypes.array,

    },
    check(value, id, event) {
        const i = `${value};${id}`;
        this.setState({
            checked: _.includes(this.state.checked, i)
                ? _.filter(this.state.checked, (v) => v !== i)
                : _.concat(this.state.checked, [i])
        })
    },
    isChecked(key,i){
        return _.includes(this.state.checked,`${key};${i}`)
    },
    componentDidMount() {
            this.setState({
                loading: true,
            });
            // FIXME: the transformation information should be available. Move these call to HierarchicalMapping?
            hierarchicalMappingChannel.request({
                topic: 'transform.get',
            }).subscribe(
                (transform) => {
                    hierarchicalMappingChannel.request(
                        {
                            topic: 'rule.suggestions',
                            data: {
                                targets: this.props.targets,
                                dataset: transform.example.selection.inputId
                            }
                        }).subscribe(
                        (response) => {
                            this.setState({
                                loading: false,
                                data: response,
                            });
                        },
                        (err) => {
                            console.warn('err MappingRuleOverview: rule.suggestions');
                            this.setState({loading: false});
                        },
                    );
                },
                (err) => {
                    console.warn('err MappingRuleOverview: transform.get');
                    this.setState({loading: false});
                }
            )
    },
    handleAddSuggestions(event) {
        event.stopPropagation();
        let remaining = this.state.checked.length;
        this.setState({
            loading: true,
        })
        _.map(this.state.checked, (suggestion) => {
            const a = _.split(suggestion, ';');
            const suggestionForAdding = this.state.data[a[0]][a[1]];
            suggestionForAdding.targetClassUri = a[0];
            // FIXME: atomize it with only 1 call to DI or stop when the first save fails?
            hierarchicalMappingChannel.request({
                topic: 'rule.createValueMapping',
                data: {
                    id: undefined,
                    parentId: this.props.id,
                    type: 'direct',
                    comment: '',
                    targetProperty: suggestionForAdding.targetClassUri,
                    propertyType: '',
                    sourceProperty: suggestionForAdding.uri,
                }
            }).subscribe(
                () => {
                    remaining--;
                    if (remaining === 0){
                        hierarchicalMappingChannel.subject('reload').onNext(true);
                        this.setState({loading: false});
                        this.props.onClose(event);
                    }

                }, (err) => {
                    this.setState({
                        error: err,
                        loading: false,
                    });
                });


        });

    },
    getInitialState() {
        return {
            data: undefined,
            checked: [],
        };
    },
    checkAll(event) {
        let checked = [];
        event.stopPropagation();
        _.map(this.state.data, (value, key) => {
            _.map(value, (e,i) => {
                checked.push(`${key};${i}`);
            })
        })
        this.setState({checked});
    },
    checkNone(event) {
        event.stopPropagation();
        this.setState({
            checked: false,
        });
    },
    // template rendering
    render () {
        const suggestionsHeader = (
            <div className="mdl-card__title mdl-card--border">
                <div className="mdl-card__title-text">
                     Add suggested mapping rules ({_.sum(_.map(this.state.data, v => v.length))})
                </div>
                <ContextMenu
                    className="ecc-silk-mapping__ruleslistmenu"
                >
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-select-all"
                        onClick={this.checkAll}
                    >
                        Select all
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-select-none"
                        onClick={this.checkNone}
                    >
                        Select none
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-tbd"
                    >
                        Select entity prop. (TODO)
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-tbd"

                    >
                        Select source matches (TODO)
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-tbd"

                    >
                        Hide unselected (TODO)
                    </MenuItem>
                </ContextMenu>
            </div>

        );


        const suggestionsList = _.isEmpty(this.state.error)
            ?_.map(this.state.data, (value, key) => {
            return _.map(value, (item, i) => <SuggestionView
                item={item}
                i={i}
                k={key}
                check={this.check}
                checked={this.isChecked(key, i)}
            />
        )});

        const actions = <div className="mdl-card__actions mdl-card__actions--fixed mdl-card--border">
            <AffirmativeButton onClick={this.handleAddSuggestions} >Save</AffirmativeButton>
            <DismissiveButton onClick={this.props.onClose} >Cancel</DismissiveButton>
        </div>

        if (this.state.loading) {
            return <Spinner/>;
        }
        else {
            return <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    {suggestionsHeader}
                    <ol className="mdl-list">
                        {suggestionsList}
                    </ol>
                    {actions}
                </div>
            </div>

        }
    }
});

export default SuggestionsView;
