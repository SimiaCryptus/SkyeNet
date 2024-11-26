import React from 'react';
import styled from 'styled-components';
import { DefaultTheme } from 'styled-components';

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
  return (
    <TabContainer>
      <TabList>
        {tabs.map(tab => (
          <TabButton
            key={tab.id}
            active={activeTab === tab.id}
            onClick={() => onTabChange(tab.id)}
          >
            {tab.label}
          </TabButton>
        ))}
      </TabList>
      {children}
    </TabContainer>
  );
};

export default Tabs;