import React, {useEffect} from 'react';
import styled, {DefaultTheme} from 'styled-components';

const LOG_PREFIX = '[Tabs]';


const TabContainer = styled.div`
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;

    .tab-content:not(.active) {
        display: none; /* Ensure inactive tab content is hidden */
    }
`;

const TabList = styled.div<{ theme: DefaultTheme }>`
    display: flex;
    flex-wrap: wrap;
    border-bottom: 1px solid ${(props: { theme: DefaultTheme }) => props.theme.colors.border};
    background: ${props => props.theme.colors.surface};
    padding: 0 1rem;
`;

const TabButton = styled.div<{ active: boolean; theme: DefaultTheme }>`
    display: inline-block;
    padding: 0.5rem 1rem;
    border: none;
    background: none;
    cursor: pointer;
    position: relative;
    font-weight: ${props => props.active ? props.theme.typography.fontWeight.bold : props.theme.typography.fontWeight.regular};
    color: ${props => props.active ? props.theme.colors.primary : props.theme.colors.text.primary};

    &:after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 2px;
        background: ${props => props.active ? props.theme.colors.primary : 'transparent'};
        transition: background-color 0.2s ease-in-out;
    }

    transition: all 0.2s ease-in-out;

    &:hover {
        background: ${props => props.theme.colors.surface};

        &:after {
            background: ${props => props.active ? props.theme.colors.primary : props.theme.colors.secondary};
        }
    }
`;

const TabContent = styled.div`
    flex: 1;
    overflow: auto;
    display: flex;
    flex-direction: column;
    padding: 1rem;
    data-tab: ${(props: { 'data-tab'?: string }) => props['data-tab']};

    .tab-content {
        animation: fadeIn 0.3s ease-in-out;
    }

    @keyframes fadeIn {
        from {
            opacity: 0;
        }
        to {
            opacity: 1;
        }
    }
`;

interface TabsProps {
    tabs: { id: string; label: string }[];
    activeTab: string;
    onTabChange: (tabId: string) => void;
    children: React.ReactNode;
}

const Tabs: React.FC<TabsProps> = ({tabs, activeTab, onTabChange, children}) => {
    useEffect(() => {
        console.group(`${LOG_PREFIX} Component Lifecycle`);
        console.log('Component mounted/updated', {
            state: {
                tabsCount: tabs.length,
                activeTab,
                availableTabs: tabs.map(t => t.id)
            }
        });

        // Restore the selected tab from localStorage
        const savedTab = localStorage.getItem('activeTab');
        if (savedTab && tabs.some(tab => tab.id === savedTab)) {
            console.log(`${LOG_PREFIX} Restoring saved tab:`, savedTab);
            onTabChange(savedTab);
        } else if (!tabs.some(tab => tab.id === activeTab)) {
            console.warn(`${LOG_PREFIX} Active tab "${activeTab}" not found in available tabs. Defaulting to first tab.`);
            onTabChange(tabs[0].id);
        }

        return () => {
            console.log(`${LOG_PREFIX} Component unmounting`);
            console.groupEnd();
        };
    }, [tabs, activeTab]);

    const handleTabClick = (tabId: string) => {
        console.group(`${LOG_PREFIX} Tab Interaction`);
        console.log('Tab clicked', {
            previousTab: activeTab,
            newTab: tabId,
            timestamp: new Date().toISOString()
        });
        onTabChange(tabId);
        localStorage.setItem('activeTab', tabId);
        console.groupEnd();
    };

    let elements = tabs.map(tab => {
        if (process.env.NODE_ENV === 'development') {
            console.debug(`${LOG_PREFIX} Rendering tab`, {
                id: tab.id,
                label: tab.label,
                active: activeTab === tab.id
            });
        }
        return (
            <TabButton
                key={tab.id}
                active={activeTab === tab.id}
                onClick={() => handleTabClick(tab.id)}
                className="tab-button"
                data-for-tab={tab.id}
            >
                {tab.label}
            </TabButton>
        )
    });
    return (
        <TabContainer>
            <TabList>
                {elements}
            </TabList>
            <TabContent data-tab={activeTab}>
                {children}
            </TabContent>
        </TabContainer>
    );
};

Tabs.displayName = 'Tabs';


export default Tabs;