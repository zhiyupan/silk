import React, { useContext } from "react";
import { HtmlContentBlock } from "@gui-elements/index";
import { ITargetWithSelected } from "../../suggestion.typings";
import { SuggestionListContext } from "../../SuggestionContainer";
import { InfoBoxOverlay } from "./InfoBoxOverlay";

interface IProps {
    source?: string | ITargetWithSelected[];
}

/** Shows additional information for a dataset source path, e.g. examples values. */
export function ExampleInfoBox({source}: IProps) {
    const context = useContext(SuggestionListContext);
    const {exampleValues, portalContainer} = context;

    let examples = [];
    if (typeof source === 'string') {
        examples = exampleValues[source as string];
    } else if (Array.isArray(source)) {
        const selected = source.find(t => t._selected);
        if (selected && exampleValues[selected.uri]) {
            examples.push(exampleValues[selected.uri]);
        }
    }

    return <InfoBoxOverlay
        data={[
            {
                key: "Example data",
                value: <code>
                    <HtmlContentBlock>
                        <ul>
                            {Array.from(new Set(examples)).sort().slice(0, 9).map((item) => {
                                    return <li>{item}</li>;
                                })}
                        </ul>
                    </HtmlContentBlock>
                </code>,
            }
        ]}
    />;
}
