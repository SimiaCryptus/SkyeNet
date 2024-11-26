import React, {useEffect} from 'react';
import styled, {DefaultTheme} from 'styled-components';

const TabContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const TabList = styled.div<{ theme: DefaultTheme }>`
  display: flex;
  border-bottom: 1px solid ${(props: { theme: DefaultTheme }) => props.theme.colors.border};
`;

const TabButton = styled.button<{ active: boolean; theme: DefaultTheme }>`
  padding: 0.5rem 1rem;
  border: none;
  background: none;
  cursor: pointer;
  border-bottom: 2px solid ${(props: { active: boolean; theme: DefaultTheme }) => 
    props.active ? props.theme.colors.primary : 'transparent'};
`;

interface TabsProps {
  tabs: { id: string; label: string }[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
  children: React.ReactNode;
}

const Tabs: React.FC<TabsProps> = ({ tabs, activeTab, onTabChange, children }) => {
  useEffect(() => {
    console.log('Tabs component mounted/updated with:', {
      tabsCount: tabs.length,
      activeTab,
      availableTabs: tabs.map(t => t.id)
    });
    return () => {
      console.log('Tabs component unmounting');
    };
  }, [tabs, activeTab]);
  const handleTabClick = (tabId: string) => {
    console.log('Tab clicked:', tabId);
    onTabChange(tabId);
  };

  return (
    <TabContainer>
      <TabList>
        {tabs.map(tab => {
          console.log('Rendering tab:', tab.id, 'active:', activeTab === tab.id);
          return (
          <TabButton
            key={tab.id}
            active={activeTab === tab.id}
            onClick={() => handleTabClick(tab.id)}
          >
            {tab.label}
          </TabButton>
          )
        })}
      </TabList>
      {children}
    </TabContainer>
  );
};

Tabs.displayName = 'Tabs';


export default Tabs;