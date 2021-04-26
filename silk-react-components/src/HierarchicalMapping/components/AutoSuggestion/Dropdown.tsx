import React from "react";
import computeScrollIntoView from "compute-scroll-into-view";
import {
    Menu,
    MenuItem,
    Highlighter,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverflowText,
    Spinner,
    Spacing,
} from "@gui-elements/index";

interface IDropdownProps {
    options: Array<any>;
    onItemSelectionChange: (item) => void;
    isOpen: boolean;
    query?: string;
    loading?: boolean;
    left?: number;
    currentlyFocusedIndex?: number;
}

const RawItem = ({ item, query }, ref) => {
    return (
        <div ref={ref}>
            <OverviewItem>
                <OverviewItemDescription>
                    <OverviewItemLine>
                        <OverflowText ellipsis="reverse">
                            <Highlighter
                                label={item.value}
                                searchValue={query}
                            ></Highlighter>
                        </OverflowText>
                    </OverviewItemLine>
                    {item.description ? (
                        <OverviewItemLine small={true}>
                            <OverflowText ellipsis="reverse">
                                <Highlighter
                                    label={item.description}
                                    searchValue={query}
                                />
                            </OverflowText>
                        </OverviewItemLine>
                    ) : null}
                </OverviewItemDescription>
            </OverviewItem>
        </div>
    );
};

const Item = React.forwardRef(RawItem);

const Dropdown: React.FC<IDropdownProps> = ({
    isOpen,
    options,
    loading,
    onItemSelectionChange,
    query,
    left,
    currentlyFocusedIndex,
}) => {
    const dropdownRef = React.useRef();
    const refs = {};
    const generateRef = (index) => {
        if (!refs.hasOwnProperty(`${index}`)) {
            refs[`${index}`] = React.createRef();
        }
        return refs[`${index}`];
    };

    React.useEffect(() => {
        const listIndexNode = refs[currentlyFocusedIndex];
        if (dropdownRef.current && listIndexNode.current) {
            const actions = computeScrollIntoView(listIndexNode.current, {
                boundary: dropdownRef.current,
                block: "nearest",
                scrollMode: "if-needed",
            });
            actions.forEach(({ el, top, left }) => {
                el.scrollTop = top;
                el.scrollLeft = left;
            });
        }
    }, [currentlyFocusedIndex]);

    if (!isOpen) return null;

    const Loader = (
        <OverviewItem hasSpacing>
            <OverviewItemLine>Fetching suggestions</OverviewItemLine>
            <Spacing size="tiny" vertical={true} />
            <Spinner position="inline" description="" />
        </OverviewItem>
    );

    return (
        <div
            className="ecc-auto-suggestion-box__dropdown"
            style={{ left }}
            ref={dropdownRef}
        >
            {loading ? (
                Loader
            ) : (
                <Menu>
                    {options.map((item, index) => (
                        <MenuItem
                            active={currentlyFocusedIndex === index}
                            key={index}
                            onMouseDown={(e) => e.preventDefault()}
                            onClick={() => onItemSelectionChange(item.value)}
                            text={
                                <Item
                                    ref={generateRef(index)}
                                    item={item}
                                    query={query}
                                />
                            }
                        ></MenuItem>
                    ))}
                </Menu>
            )}
        </div>
    );
};

export default Dropdown;
